//! Rust replacement for Ampy's native rolling power statistics library.
//!
//! Exports the same four JNI functions as the previous C++ implementation.

use jni::objects::JObject;
use jni::sys::{jfloat, jfloatArray, jint, jlong};
use jni::JNIEnv;
use std::collections::VecDeque;
use std::ptr;

#[derive(Clone, Copy)]
struct Summary {
    sum: f64,
    average: f64,
    min: f32,
    max: f32,
    count: usize,
}

struct RollingPowerStats {
    capacity: usize,
    values: Vec<f32>,
    count: usize,
    sum: f64,
    sequence: u64,
    min_deque: VecDeque<(u64, f32)>,
    max_deque: VecDeque<(u64, f32)>,
}

impl RollingPowerStats {
    fn new(capacity: usize) -> Self {
        let capacity = capacity.max(1);
        Self {
            capacity,
            values: vec![0.0; capacity],
            count: 0,
            sum: 0.0,
            sequence: 0,
            min_deque: VecDeque::new(),
            max_deque: VecDeque::new(),
        }
    }

    fn expire(deque: &mut VecDeque<(u64, f32)>, sequence: u64, capacity: usize) {
        while deque
            .front()
            .is_some_and(|front| front.0.saturating_add(capacity as u64) <= sequence)
        {
            deque.pop_front();
        }
    }

    fn push(&mut self, value: f32) -> Summary {
        let sequence = self.sequence;
        self.sequence = self.sequence.wrapping_add(1);
        let slot = (sequence % self.capacity as u64) as usize;

        if self.count < self.capacity {
            self.count += 1;
        } else {
            self.sum -= self.values[slot] as f64;
        }
        self.values[slot] = value;
        self.sum += value as f64;

        while self.min_deque.back().is_some_and(|back| back.1 >= value) {
            self.min_deque.pop_back();
        }
        self.min_deque.push_back((sequence, value));
        Self::expire(&mut self.min_deque, sequence, self.capacity);

        while self.max_deque.back().is_some_and(|back| back.1 <= value) {
            self.max_deque.pop_back();
        }
        self.max_deque.push_back((sequence, value));
        Self::expire(&mut self.max_deque, sequence, self.capacity);

        Summary {
            sum: self.sum,
            average: self.sum / self.count as f64,
            min: self.min_deque.front().map(|entry| entry.1).unwrap_or(value),
            max: self.max_deque.front().map(|entry| entry.1).unwrap_or(value),
            count: self.count,
        }
    }

    fn reset(&mut self) {
        self.values.fill(0.0);
        self.min_deque.clear();
        self.max_deque.clear();
        self.count = 0;
        self.sum = 0.0;
        self.sequence = 0;
    }
}

unsafe fn from_handle(handle: jlong) -> Option<&'static mut RollingPowerStats> {
    if handle <= 0 {
        return None;
    }
    let pointer = handle as *mut RollingPowerStats;
    if pointer.is_null() {
        None
    } else {
        Some(unsafe { &mut *pointer })
    }
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyStatsNative_create<'local>(
    _env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    capacity: jint,
) -> jlong {
    let capacity = if capacity > 0 { capacity as usize } else { 1 };
    Box::into_raw(Box::new(RollingPowerStats::new(capacity))) as jlong
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyStatsNative_push<'local>(
    env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    handle: jlong,
    value: jfloat,
) -> jfloatArray {
    let stats = match unsafe { from_handle(handle) } {
        Some(stats) => stats,
        None => return ptr::null_mut(),
    };
    let summary = stats.push(value);
    let packed: [jfloat; 5] = [
        summary.sum as f32,
        summary.average as f32,
        summary.min,
        summary.max,
        summary.count as f32,
    ];

    let result = match env.new_float_array(5) {
        Ok(result) => result,
        Err(_) => return ptr::null_mut(),
    };
    if env.set_float_array_region(&result, 0, &packed).is_err() {
        return ptr::null_mut();
    }
    result.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyStatsNative_reset<'local>(
    _env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    handle: jlong,
) {
    if let Some(stats) = unsafe { from_handle(handle) } {
        stats.reset();
    }
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyStatsNative_destroy<'local>(
    _env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    handle: jlong,
) {
    if handle <= 0 {
        return;
    }
    unsafe {
        drop(Box::from_raw(handle as *mut RollingPowerStats));
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rolling_window_matches_cpp_behaviour() {
        let mut stats = RollingPowerStats::new(3);
        let mut s = stats.push(1.0);
        assert_eq!(s.count, 1);
        assert!((s.average - 1.0).abs() < 0.001);
        assert_eq!((s.min, s.max), (1.0, 1.0));

        s = stats.push(5.0);
        assert_eq!(s.count, 2);
        assert!((s.average - 3.0).abs() < 0.001);
        assert_eq!((s.min, s.max), (1.0, 5.0));

        s = stats.push(2.0);
        assert_eq!(s.count, 3);
        assert!((s.average - 8.0 / 3.0).abs() < 0.001);
        assert_eq!((s.min, s.max), (1.0, 5.0));

        s = stats.push(9.0);
        assert_eq!(s.count, 3);
        assert!((s.average - 16.0 / 3.0).abs() < 0.001);
        assert_eq!((s.min, s.max), (2.0, 9.0));

        s = stats.push(0.0);
        assert_eq!(s.count, 3);
        assert!((s.average - 11.0 / 3.0).abs() < 0.001);
        assert_eq!((s.min, s.max), (0.0, 9.0));

        stats.reset();
        s = stats.push(7.0);
        assert_eq!(s.count, 1);
        assert_eq!((s.min, s.max), (7.0, 7.0));
    }
}
