//! Rust replacement for Ampy's native chart downsampler.
//!
//! The JNI surface is intentionally identical to the previous C++ library:
//! `libampy_chart.so` exports
//! `Java_com_zenin_ampy_nativelib_AmpyChartNative_downsample`.

use jni::objects::{JFloatArray, JLongArray, JObject};
use jni::sys::{jfloatArray, jint, jlongArray, jsize};
use jni::JNIEnv;
use std::ptr;

/// Port of `ampy::chart::downsampleSeries`.
///
/// Output is packed triads: `[relative_time_ms, power, temperature]`.
fn downsample_series(
    timestamps: &[i64],
    power: &[f32],
    temperature: &[f32],
    buckets: i32,
) -> Vec<f32> {
    let size = timestamps.len();
    if size == 0 {
        return Vec::new();
    }

    let base = timestamps[0];
    if buckets <= 0 || size <= buckets as usize {
        let mut packed = Vec::with_capacity(size * 3);
        for i in 0..size {
            packed.push((timestamps[i] - base) as f32);
            packed.push(power[i]);
            packed.push(temperature[i]);
        }
        return packed;
    }

    let last = timestamps[size - 1];
    let range = (last - base).max(1);
    let bucket_count = buckets.min(size as i32);

    #[derive(Clone, Copy)]
    struct Bucket {
        has_point: bool,
        min_index: usize,
        max_index: usize,
    }

    let mut buckets_state = vec![
        Bucket {
            has_point: false,
            min_index: 0,
            max_index: 0,
        };
        bucket_count as usize
    ];

    for i in 0..size {
        let mut index = ((timestamps[i] - base) * bucket_count as i64 / range) as i32;
        if index < 0 {
            index = 0;
        }
        if index >= bucket_count {
            index = bucket_count - 1;
        }

        let bucket = &mut buckets_state[index as usize];
        if !bucket.has_point {
            bucket.has_point = true;
            bucket.min_index = i;
            bucket.max_index = i;
            continue;
        }
        if power[i] < power[bucket.min_index] {
            bucket.min_index = i;
        }
        if power[i] > power[bucket.max_index] {
            bucket.max_index = i;
        }
    }

    let mut packed = Vec::with_capacity(bucket_count as usize * 6);
    for bucket in buckets_state {
        if !bucket.has_point {
            continue;
        }
        let (first, second) = if bucket.min_index <= bucket.max_index {
            (bucket.min_index, bucket.max_index)
        } else {
            (bucket.max_index, bucket.min_index)
        };

        packed.push((timestamps[first] - base) as f32);
        packed.push(power[first]);
        packed.push(temperature[first]);
        if second != first {
            packed.push((timestamps[second] - base) as f32);
            packed.push(power[second]);
            packed.push(temperature[second]);
        }
    }
    packed
}

fn downsample_impl(
    env: &mut JNIEnv<'_>,
    timestamps: jlongArray,
    power: jfloatArray,
    temperature: jfloatArray,
    buckets: jint,
) -> Result<jfloatArray, ()> {
    if timestamps.is_null() || power.is_null() || temperature.is_null() {
        return Err(());
    }

    let timestamps = unsafe { JLongArray::from_raw(timestamps) };
    let power = unsafe { JFloatArray::from_raw(power) };
    let temperature = unsafe { JFloatArray::from_raw(temperature) };

    let count = env.get_array_length(&timestamps).map_err(|_| ())?;
    if count != env.get_array_length(&power).map_err(|_| ())?
        || count != env.get_array_length(&temperature).map_err(|_| ())?
    {
        return Err(());
    }

    let mut native_timestamps = vec![0i64; count as usize];
    let mut native_power = vec![0f32; count as usize];
    let mut native_temperature = vec![0f32; count as usize];
    env.get_long_array_region(&timestamps, 0, &mut native_timestamps)
        .map_err(|_| ())?;
    env.get_float_array_region(&power, 0, &mut native_power)
        .map_err(|_| ())?;
    env.get_float_array_region(&temperature, 0, &mut native_temperature)
        .map_err(|_| ())?;

    let packed = downsample_series(
        &native_timestamps,
        &native_power,
        &native_temperature,
        buckets,
    );
    if packed.is_empty() {
        return Err(());
    }

    let result = env.new_float_array(packed.len() as jsize).map_err(|_| ())?;
    env.set_float_array_region(&result, 0, &packed)
        .map_err(|_| ())?;
    Ok(result.into_raw())
}

#[no_mangle]
pub extern "C" fn Java_com_zenin_ampy_nativelib_AmpyChartNative_downsample<'local>(
    mut env: JNIEnv<'local>,
    _thiz: JObject<'local>,
    timestamps: jlongArray,
    power: jfloatArray,
    temperature: jfloatArray,
    buckets: jint,
) -> jfloatArray {
    downsample_impl(&mut env, timestamps, power, temperature, buckets)
        .unwrap_or_else(|_| ptr::null_mut())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn keeps_all_points_when_buckets_are_not_needed() {
        let packed = downsample_series(&[100, 200], &[1.0, 2.0], &[30.0, 31.0], 0);
        assert_eq!(packed, vec![0.0, 1.0, 30.0, 100.0, 2.0, 31.0]);
    }

    #[test]
    fn downsampling_keeps_the_peak() {
        let times: Vec<i64> = (0..100).map(|i| i * 1000).collect();
        let power: Vec<f32> = (0..100)
            .map(|i| if i == 42 { 900.0 } else { i as f32 })
            .collect();
        let temperature: Vec<f32> = (0..100).map(|i| 30.0 + i as f32 / 10.0).collect();

        let packed = downsample_series(&times, &power, &temperature, 10);
        assert!(!packed.is_empty());
        assert!(packed.len() <= 60);
        assert!(packed.chunks_exact(3).any(|triad| triad[1] == 900.0));
    }
}
