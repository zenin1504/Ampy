#include <algorithm>
#include <cstdint>
#include <string>
#include <utility>
#include <vector>

#ifndef AMPY_NATIVE_STANDALONE_TEST
#include <jni.h>
#endif

namespace ampy::logparser {

using U16 = std::u16string;
using ParsedMap = std::vector<std::pair<U16, U16>>;

enum Mode {
    MODE_XIAOMI = 1,
    MODE_OPLUS = 2,
    MODE_FALLBACK = 3,
    MODE_MOTO = 4
};

inline bool isAsciiWord(char16_t c) {
    return (c >= u'a' && c <= u'z') || (c >= u'A' && c <= u'Z') ||
           (c >= u'0' && c <= u'9') || c == u'_';
}

inline bool isAsciiSpace(char16_t c) {
    return c == u' ' || c == u'\t' || c == u'\n' || c == u'\r' ||
           c == u'\f' || c == u'\v';
}

inline char16_t asciiLower(char16_t c) {
    return (c >= u'A' && c <= u'Z') ? static_cast<char16_t>(c + 32) : c;
}

U16 trim(const U16& value) {
    std::size_t start = 0;
    std::size_t end = value.size();
    while (start < end && isAsciiSpace(value[start])) ++start;
    while (end > start && isAsciiSpace(value[end - 1])) --end;
    return value.substr(start, end - start);
}

std::size_t findCaseInsensitive(const U16& haystack, const U16& needle,
                                std::size_t from = 0) {
    if (needle.empty()) return from;
    for (std::size_t i = from; i + needle.size() <= haystack.size(); ++i) {
        bool equal = true;
        for (std::size_t j = 0; j < needle.size(); ++j) {
            if (asciiLower(haystack[i + j]) != asciiLower(needle[j])) {
                equal = false;
                break;
            }
        }
        if (equal) return i;
    }
    return U16::npos;
}

void putPair(ParsedMap& out, U16 key, U16 value) {
    key = trim(key);
    value = trim(value);
    if (key.empty()) return;
    for (auto& entry : out) {
        if (entry.first == key) {
            entry.second = std::move(value);
            return;
        }
    }
    out.emplace_back(std::move(key), std::move(value));
}

U16 afterFirstColon(const U16& line) {
    const std::size_t pos = line.find(u':');
    return pos == U16::npos ? line : line.substr(pos + 1);
}

U16 afterLastColon(const U16& line) {
    const std::size_t pos = line.rfind(u':');
    return pos == U16::npos ? line : line.substr(pos + 1);
}

ParsedMap parseXiaomi(const U16& line) {
    const U16 payload = trim(afterFirstColon(line));
    ParsedMap out;

    for (std::size_t i = 0; i < payload.size();) {
        if (!isAsciiWord(payload[i])) {
            ++i;
            continue;
        }
        const std::size_t keyStart = i;
        while (i < payload.size() && isAsciiWord(payload[i])) ++i;
        if (i >= payload.size() || payload[i] != u'=') continue;

        const U16 key = payload.substr(keyStart, i - keyStart);
        const std::size_t valueStart = ++i;
        while (i < payload.size() && !isAsciiSpace(payload[i]) && payload[i] != u',') ++i;
        if (i > valueStart) {
            putPair(out, key, payload.substr(valueStart, i - valueStart));
        }
    }
    return out;
}

ParsedMap parseOPlus(const U16& line) {
    ParsedMap out;

    for (std::size_t i = 0; i < line.size();) {
        if (!isAsciiWord(line[i]) || (i > 0 && isAsciiWord(line[i - 1]))) {
            ++i;
            continue;
        }
        const std::size_t keyStart = i;
        while (i < line.size() && isAsciiWord(line[i])) ++i;
        if (i >= line.size() || line[i] != u'=') continue;

        const U16 key = line.substr(keyStart, i - keyStart);
        const std::size_t valueStart = ++i;
        while (i < line.size() && line[i] != u',') ++i;
        putPair(out, key, line.substr(valueStart, i - valueStart));
        if (i < line.size() && line[i] == u',') ++i;
    }
    return out;
}

ParsedMap parseSplitPayload(const U16& payload, char16_t delimiter, bool splitOnSpaces) {
    ParsedMap out;
    std::size_t start = 0;
    for (std::size_t i = 0; i <= payload.size(); ++i) {
        const bool isEnd = i == payload.size();
        if (!isEnd && payload[i] != delimiter &&
            !(splitOnSpaces && isAsciiSpace(payload[i]))) continue;

        const U16 segment = payload.substr(start, i - start);
        start = i + 1;
        const std::size_t equals = segment.find(u'=');
        if (equals == U16::npos) continue;
        putPair(out, segment.substr(0, equals), segment.substr(equals + 1));
    }
    return out;
}

ParsedMap parseFallback(const U16& line) {
    return parseSplitPayload(trim(afterLastColon(line)), u',', true);
}

bool readMotoPayload(const U16& line, U16& payload) {
    std::size_t pos = findCaseInsensitive(line, u"uevent");
    if (pos == U16::npos) return false;

    pos += 6;
    while (pos < line.size() && isAsciiSpace(line[pos])) ++pos;
    if (pos >= line.size() || line[pos] != u'=') return false;
    ++pos;
    while (pos < line.size() && isAsciiSpace(line[pos])) ++pos;
    if (pos >= line.size() || line[pos] != u'{') return false;

    const std::size_t close = line.find(u'}', pos + 1);
    if (close == U16::npos) return false;
    payload = line.substr(pos + 1, close - pos - 1);
    return true;
}

ParsedMap parseMoto(const U16& line) {
    U16 payload;
    if (!readMotoPayload(line, payload)) return {};
    return parseSplitPayload(payload, u',', false);
}

bool matchesXiaomi(const U16& line) {
    const std::size_t pos = line.find(u"chargerType=");
    return pos != U16::npos && (pos == 0 || !isAsciiWord(line[pos - 1]));
}

bool matchesOPlus(const U16& line) {
    std::size_t pos = 0;
    while ((pos = findCaseInsensitive(line, u"type", pos)) != U16::npos) {
        if (pos > 0 && isAsciiWord(line[pos - 1])) {
            pos += 4;
            continue;
        }
        std::size_t cursor = pos + 4;
        while (cursor < line.size() && isAsciiSpace(line[cursor])) ++cursor;
        if (cursor < line.size() && line[cursor] == u'=') return true;
        pos += 4;
    }
    return false;
}

bool matchesFallback(const U16& line) {
    return line.find(u'=') != U16::npos &&
           line.find(u"BatteryService") == U16::npos &&
           line.find(u"charge_time:") == U16::npos;
}

bool matchesMoto(const U16& line) {
    U16 payload;
    return readMotoPayload(line, payload);
}

struct Spec {
    U16 marker;
    int mode;
};

bool matchesSpec(const U16& line, const Spec& spec) {
    // Mode defines the exact matcher; marker is kept for Kotlin-side parity.
    (void)spec.marker;
    switch (spec.mode) {
        case MODE_XIAOMI: return matchesXiaomi(line);
        case MODE_OPLUS: return matchesOPlus(line);
        case MODE_FALLBACK: return matchesFallback(line);
        case MODE_MOTO: return matchesMoto(line);
        default: return false;
    }
}

ParsedMap parseSpec(const U16& line, const Spec& spec) {
    switch (spec.mode) {
        case MODE_XIAOMI: return parseXiaomi(line);
        case MODE_OPLUS: return parseOPlus(line);
        case MODE_FALLBACK: return parseFallback(line);
        case MODE_MOTO: return parseMoto(line);
        default: return {};
    }
}

}  // namespace ampy::logparser

#ifndef AMPY_NATIVE_STANDALONE_TEST

static jobject toJavaMap(JNIEnv* env, const ampy::logparser::ParsedMap& parsed) {
    jclass mapClass = env->FindClass("java/util/LinkedHashMap");
    if (mapClass == nullptr) return nullptr;
    jmethodID constructor = env->GetMethodID(mapClass, "<init>", "()V");
    jmethodID put = env->GetMethodID(
        mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    env->DeleteLocalRef(mapClass);
    if (constructor == nullptr || put == nullptr) return nullptr;

    jobject map = env->NewObject(mapClass, constructor);
    if (map == nullptr) return nullptr;

    for (const auto& entry : parsed) {
        jstring key = env->NewString(
            reinterpret_cast<const jchar*>(entry.first.data()),
            static_cast<jsize>(entry.first.size()));
        jstring value = env->NewString(
            reinterpret_cast<const jchar*>(entry.second.data()),
            static_cast<jsize>(entry.second.size()));
        if (key == nullptr || value == nullptr) {
            if (key != nullptr) env->DeleteLocalRef(key);
            if (value != nullptr) env->DeleteLocalRef(value);
            env->DeleteLocalRef(map);
            return nullptr;
        }
        env->CallObjectMethod(map, put, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
    }
    return map;
}

static ampy::logparser::U16 readU16(JNIEnv* env, jstring value) {
    const jchar* chars = env->GetStringChars(value, nullptr);
    if (chars == nullptr) return {};
    const jsize length = env->GetStringLength(value);
    ampy::logparser::U16 result(chars, chars + length);
    env->ReleaseStringChars(value, chars);
    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_zenin_ampy_nativelib_AmpyLogNative_parseFirst(
        JNIEnv* env, jobject /*thiz*/,
        jobjectArray lines, jobjectArray markers, jintArray modes) {
    if (lines == nullptr || markers == nullptr || modes == nullptr) return nullptr;

    const jsize lineCount = env->GetArrayLength(lines);
    const jsize markerCount = env->GetArrayLength(markers);
    const jsize modeCount = env->GetArrayLength(modes);
    const jsize specCount = std::min(markerCount, modeCount);
    if (specCount == 0) return nullptr;

    std::vector<ampy::logparser::Spec> specs;
    specs.reserve(specCount);
    jint* rawModes = env->GetIntArrayElements(modes, nullptr);
    if (rawModes == nullptr) return nullptr;

    for (jsize i = 0; i < specCount; ++i) {
        jstring marker = static_cast<jstring>(env->GetObjectArrayElement(markers, i));
        if (marker != nullptr) {
            specs.push_back({readU16(env, marker), rawModes[i]});
            env->DeleteLocalRef(marker);
        }
    }
    env->ReleaseIntArrayElements(modes, rawModes, JNI_ABORT);

    for (jsize lineIndex = lineCount - 1; lineIndex >= 0; --lineIndex) {
        jstring lineRef = static_cast<jstring>(env->GetObjectArrayElement(lines, lineIndex));
        if (lineRef == nullptr) continue;

        const ampy::logparser::U16 line = readU16(env, lineRef);
        env->DeleteLocalRef(lineRef);

        for (const auto& spec : specs) {
            if (!ampy::logparser::matchesSpec(line, spec)) continue;
            const ampy::logparser::ParsedMap parsed = ampy::logparser::parseSpec(line, spec);
            if (!parsed.empty()) return toJavaMap(env, parsed);
        }
    }
    return nullptr;
}

#else

#include <cassert>
#include <iostream>

static ampy::logparser::U16 asciiU16(const char* value) {
    ampy::logparser::U16 result;
    while (*value != '\0') result.push_back(static_cast<char16_t>(*value++));
    return result;
}

static ampy::logparser::U16 valueOf(
        const ampy::logparser::ParsedMap& map, const char* key) {
    const ampy::logparser::U16 wanted = asciiU16(key);
    for (const auto& entry : map) {
        if (entry.first == wanted) return entry.second;
    }
    return u"";
}

int main() {
    using namespace ampy::logparser;

    const U16 xiaomi = asciiU16(
        "D DeviceStatisticsService: chargerType=100, batteryLevel=85, totalBatteryCapacity=4007300");
    assert(matchesXiaomi(xiaomi));
    assert(valueOf(parseXiaomi(xiaomi), "totalBatteryCapacity") == u"4007300");

    const U16 oplus = asciiU16(
        "D charge_time: type=charging, fcc=4500, batteryLevel=70");
    assert(matchesOPlus(oplus));
    assert(valueOf(parseOPlus(oplus), "fcc") == u"4500");

    const U16 moto = asciiU16(
        "D BatteryService: uevent={POWER_SUPPLY_VOLTAGE_NOW=3900000,POWER_SUPPLY_CURRENT_NOW=-120000}");
    assert(matchesMoto(moto));
    assert(valueOf(parseMoto(moto), "POWER_SUPPLY_VOLTAGE_NOW") == u"3900000");

    const U16 fallback = asciiU16("D SomeServiceX: voltage_now=3900000 current_now=-120000");
    assert(matchesFallback(fallback));
    assert(valueOf(parseFallback(fallback), "voltage_now") == u"3900000");

    std::cout << "ampy log parser ok\n";
    return 0;
}

#endif
