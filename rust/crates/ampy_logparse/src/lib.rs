//! Rust replacement for Ampy's native battery log parser.
//!
//! Exports the same JNI function as the previous C++ library:
//! `Java_com_zenin_ampy_nativelib_AmpyLogNative_parseFirst`.

use jni::objects::{JIntArray, JMap, JObject, JObjectArray, JString};
use jni::sys::{jintArray, jobject, jobjectArray};
use jni::JNIEnv;
use std::ptr;

type U16 = Vec<u16>;
type ParsedMap = Vec<(U16, U16)>;

const MODE_XIAOMI: i32 = 1;
const MODE_OPLUS: i32 = 2;
const MODE_FALLBACK: i32 = 3;
const MODE_MOTO: i32 = 4;

fn is_ascii_word(c: u16) -> bool {
    (b'a' as u16..=b'z' as u16).contains(&c)
        || (b'A' as u16..=b'Z' as u16).contains(&c)
        || (b'0' as u16..=b'9' as u16).contains(&c)
        || c == b'_' as u16
}

fn is_ascii_space(c: u16) -> bool {
    matches!(c, 0x20 | 0x09 | 0x0a | 0x0d | 0x0c | 0x0b)
}

fn ascii_lower(c: u16) -> u16 {
    if (b'A' as u16..=b'Z' as u16).contains(&c) {
        c + 32
    } else {
        c
    }
}

fn trim(value: &[u16]) -> U16 {
    let mut start = 0;
    let mut end = value.len();
    while start < end && is_ascii_space(value[start]) {
        start += 1;
    }
    while end > start && is_ascii_space(value[end - 1]) {
        end -= 1;
    }
    value[start..end].to_vec()
}

fn find_case_insensitive(haystack: &[u16], needle: &[u16], from: usize) -> Option<usize> {
    if needle.is_empty() {
        return Some(from);
    }
    for i in from..=haystack.len().saturating_sub(needle.len()) {
        if (0..needle.len()).all(|j| ascii_lower(haystack[i + j]) == ascii_lower(needle[j])) {
            return Some(i);
        }
    }
    None
}

fn put_pair(out: &mut ParsedMap, key: U16, value: U16) {
    let key = trim(&key);
    let value = trim(&value);
    if key.is_empty() {
        return;
    }
    if let Some(entry) = out.iter_mut().find(|entry| entry.0 == key) {
        entry.1 = value;
        return;
    }
    out.push((key, value));
}

fn after_first_colon(line: &[u16]) -> U16 {
    match line.iter().position(|&c| c == b':' as u16) {
        Some(pos) => line[pos + 1..].to_vec(),
        None => line.to_vec(),
    }
}

fn after_last_colon(line: &[u16]) -> U16 {
    match line.iter().rposition(|&c| c == b':' as u16) {
        Some(pos) => line[pos + 1..].to_vec(),
        None => line.to_vec(),
    }
}

fn parse_xiaomi(line: &[u16]) -> ParsedMap {
    let payload = trim(&after_first_colon(line));
    let mut out = ParsedMap::new();
    let mut i = 0;

    while i < payload.len() {
        if !is_ascii_word(payload[i]) {
            i += 1;
            continue;
        }
        let key_start = i;
        while i < payload.len() && is_ascii_word(payload[i]) {
            i += 1;
        }
        if i >= payload.len() || payload[i] != b'=' as u16 {
            continue;
        }

        let key = payload[key_start..i].to_vec();
        i += 1;
        let value_start = i;
        while i < payload.len() && !is_ascii_space(payload[i]) && payload[i] != b',' as u16 {
            i += 1;
        }
        if i > value_start {
            put_pair(&mut out, key, payload[value_start..i].to_vec());
        }
    }
    out
}

fn parse_oplus(line: &[u16]) -> ParsedMap {
    let mut out = ParsedMap::new();
    let mut i = 0;

    while i < line.len() {
        if !is_ascii_word(line[i]) || (i > 0 && is_ascii_word(line[i - 1])) {
            i += 1;
            continue;
        }
        let key_start = i;
        while i < line.len() && is_ascii_word(line[i]) {
            i += 1;
        }
        if i >= line.len() || line[i] != b'=' as u16 {
            continue;
        }

        let key = line[key_start..i].to_vec();
        i += 1;
        let value_start = i;
        while i < line.len() && line[i] != b',' as u16 {
            i += 1;
        }
        put_pair(&mut out, key, line[value_start..i].to_vec());
        if i < line.len() && line[i] == b',' as u16 {
            i += 1;
        }
    }
    out
}

fn parse_split_payload(payload: &[u16], delimiter: u16, split_on_spaces: bool) -> ParsedMap {
    let mut out = ParsedMap::new();
    let mut start = 0;

    for i in 0..=payload.len() {
        let is_end = i == payload.len();
        if !is_end && payload[i] != delimiter && !(split_on_spaces && is_ascii_space(payload[i])) {
            continue;
        }

        let segment = &payload[start..i];
        start = i + 1;
        if let Some(equals) = segment.iter().position(|&c| c == b'=' as u16) {
            put_pair(
                &mut out,
                segment[..equals].to_vec(),
                segment[equals + 1..].to_vec(),
            );
        }
    }
    out
}

fn parse_fallback(line: &[u16]) -> ParsedMap {
    parse_split_payload(&trim(&after_last_colon(line)), b',' as u16, true)
}

fn read_moto_payload(line: &[u16]) -> Option<U16> {
    let mut pos = find_case_insensitive(line, &ascii_u16("uevent"), 0)?;
    pos += 6;
    while pos < line.len() && is_ascii_space(line[pos]) {
        pos += 1;
    }
    if pos >= line.len() || line[pos] != b'=' as u16 {
        return None;
    }
    pos += 1;
    while pos < line.len() && is_ascii_space(line[pos]) {
        pos += 1;
    }
    if pos >= line.len() || line[pos] != b'{' as u16 {
        return None;
    }

    let close = line[pos + 1..].iter().position(|&c| c == b'}' as u16)? + pos + 1;
    Some(line[pos + 1..close].to_vec())
}

fn parse_moto(line: &[u16]) -> ParsedMap {
    let payload = match read_moto_payload(line) {
        Some(payload) => payload,
        None => return ParsedMap::new(),
    };
    parse_split_payload(&payload, b',' as u16, false)
}

fn matches_xiaomi(line: &[u16]) -> bool {
    match line
        .windows(12)
        .position(|window| window == ascii_u16("chargerType="))
    {
        Some(pos) => pos == 0 || !is_ascii_word(line[pos - 1]),
        None => false,
    }
}

fn matches_oplus(line: &[u16]) -> bool {
    let needle = ascii_u16("type");
    let mut pos = 0;
    while let Some(found) = find_case_insensitive(line, &needle, pos) {
        if found > 0 && is_ascii_word(line[found - 1]) {
            pos = found + 4;
            continue;
        }
        let mut cursor = found + 4;
        while cursor < line.len() && is_ascii_space(line[cursor]) {
            cursor += 1;
        }
        if cursor < line.len() && line[cursor] == b'=' as u16 {
            return true;
        }
        pos = found + 4;
    }
    false
}

fn matches_fallback(line: &[u16]) -> bool {
    line.contains(&(b'=' as u16))
        && line
            .windows(14)
            .all(|window| window != ascii_u16("BatteryService"))
        && line
            .windows(12)
            .all(|window| window != ascii_u16("charge_time:"))
}

fn matches_moto(line: &[u16]) -> bool {
    read_moto_payload(line).is_some()
}

fn matches_spec(line: &[u16], mode: i32) -> bool {
    match mode {
        MODE_XIAOMI => matches_xiaomi(line),
        MODE_OPLUS => matches_oplus(line),
        MODE_FALLBACK => matches_fallback(line),
        MODE_MOTO => matches_moto(line),
        _ => false,
    }
}

fn parse_spec(line: &[u16], mode: i32) -> ParsedMap {
    match mode {
        MODE_XIAOMI => parse_xiaomi(line),
        MODE_OPLUS => parse_oplus(line),
        MODE_FALLBACK => parse_fallback(line),
        MODE_MOTO => parse_moto(line),
        _ => ParsedMap::new(),
    }
}

fn ascii_u16(value: &str) -> U16 {
    value.bytes().map(|byte| byte as u16).collect()
}

fn read_u16(env: &mut JNIEnv<'_>, obj: &JObject<'_>) -> Result<U16, ()> {
    if obj.as_raw().is_null() {
        return Ok(U16::new());
    }
    let jstring = unsafe { JString::from_raw(obj.as_raw()) };
    let java_str = env.get_string(&jstring).map_err(|_| ())?;
    let rust: String = java_str.into();
    Ok(rust.encode_utf16().collect())
}

fn to_java_map(env: &mut JNIEnv<'_>, parsed: &ParsedMap) -> Result<jobject, ()> {
    let map = env
        .new_object("java/util/LinkedHashMap", "()V", &[])
        .map_err(|_| ())?;
    let jmap = JMap::from_env(env, &map).map_err(|_| ())?;

    for (key, value) in parsed {
        let key_obj = JObject::from(
            env.new_string(String::from_utf16_lossy(key))
                .map_err(|_| ())?,
        );
        let value_obj = JObject::from(
            env.new_string(String::from_utf16_lossy(value))
                .map_err(|_| ())?,
        );
        jmap.put(env, &key_obj, &value_obj).map_err(|_| ())?;
    }
    Ok(map.into_raw())
}

fn parse_first_impl(
    env: &mut JNIEnv<'_>,
    lines: jobjectArray,
    markers: jobjectArray,
    modes: jintArray,
) -> Result<jobject, ()> {
    if lines.is_null() || markers.is_null() || modes.is_null() {
        return Err(());
    }

    let lines = unsafe { JObjectArray::from_raw(lines) };
    let markers = unsafe { JObjectArray::from_raw(markers) };
    let modes = unsafe { JIntArray::from_raw(modes) };

    let line_count = env.get_array_length(&lines).map_err(|_| ())?;
    let marker_count = env.get_array_length(&markers).map_err(|_| ())?;
    let mode_count = env.get_array_length(&modes).map_err(|_| ())?;
    let spec_count = marker_count.min(mode_count);
    if spec_count == 0 {
        return Err(());
    }

    let mut raw_modes = vec![0i32; spec_count as usize];
    env.get_int_array_region(&modes, 0, &mut raw_modes)
        .map_err(|_| ())?;

    let mut specs: Vec<(i32, U16)> = Vec::with_capacity(spec_count as usize);
    for i in 0..spec_count {
        let marker = env.get_object_array_element(&markers, i).map_err(|_| ())?;
        if !marker.as_raw().is_null() {
            specs.push((raw_modes[i as usize], read_u16(env, &marker)?));
        }
    }

    for line_index in (0..line_count).rev() {
        let line_ref = env
            .get_object_array_element(&lines, line_index)
            .map_err(|_| ())?;
        if line_ref.as_raw().is_null() {
            continue;
        }
        let line = read_u16(env, &line_ref)?;

        for (mode, _marker) in &specs {
            if !matches_spec(&line, *mode) {
                continue;
            }
            let parsed = parse_spec(&line, *mode);
            if !parsed.is_empty() {
                return to_java_map(env, &parsed);
            }
        }
    }
    Err(())
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyLogNative_parseFirst<'local>(
    mut env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    lines: jobjectArray,
    markers: jobjectArray,
    modes: jintArray,
) -> jobject {
    parse_first_impl(&mut env, lines, markers, modes).unwrap_or_else(|_| ptr::null_mut())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn value_of(map: &ParsedMap, key: &str) -> U16 {
        let wanted = ascii_u16(key);
        map.iter()
            .find(|entry| entry.0 == wanted)
            .map(|entry| entry.1.clone())
            .unwrap_or_default()
    }

    #[test]
    fn parses_xiaomi_oplus_moto_and_fallback_lines() {
        let xiaomi = ascii_u16(
            "D DeviceStatisticsService: chargerType=100, batteryLevel=85, totalBatteryCapacity=4007300",
        );
        assert!(matches_xiaomi(&xiaomi));
        assert_eq!(
            value_of(&parse_xiaomi(&xiaomi), "totalBatteryCapacity"),
            ascii_u16("4007300")
        );

        let oplus = ascii_u16("D charge_time: type=charging, fcc=4500, batteryLevel=70");
        assert!(matches_oplus(&oplus));
        assert_eq!(value_of(&parse_oplus(&oplus), "fcc"), ascii_u16("4500"));

        let moto = ascii_u16(
            "D BatteryService: uevent={POWER_SUPPLY_VOLTAGE_NOW=3900000,POWER_SUPPLY_CURRENT_NOW=-120000}",
        );
        assert!(matches_moto(&moto));
        assert_eq!(
            value_of(&parse_moto(&moto), "POWER_SUPPLY_VOLTAGE_NOW"),
            ascii_u16("3900000")
        );

        let fallback = ascii_u16("D SomeServiceX: voltage_now=3900000 current_now=-120000");
        assert!(matches_fallback(&fallback));
        assert_eq!(
            value_of(&parse_fallback(&fallback), "voltage_now"),
            ascii_u16("3900000")
        );
    }
}
