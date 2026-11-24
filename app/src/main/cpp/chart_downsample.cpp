#include <algorithm>
#include <cstdint>
#include <utility>
#include <vector>

#ifndef AMPY_NATIVE_STANDALONE_TEST
#include <jni.h>
#endif

namespace ampy::chart {

std::vector<float> downsampleSeries(const std::vector<std::int64_t>& timestamps,
                                    const std::vector<float>& power,
                                    const std::vector<float>& temperature,
                                    int buckets) {
    const std::size_t size = timestamps.size();
    if (size == 0) return {};

    const std::int64_t base = timestamps.front();
    if (buckets <= 0 || size <= static_cast<std::size_t>(buckets)) {
        std::vector<float> packed(size * 3);
        for (std::size_t i = 0; i < size; ++i) {
            packed[i * 3] = static_cast<float>(timestamps[i] - base);
            packed[i * 3 + 1] = power[i];
            packed[i * 3 + 2] = temperature[i];
        }
        return packed;
    }

    const std::int64_t last = timestamps.back();
    const std::int64_t range = std::max<std::int64_t>(1, last - base);
    const int bucketCount = std::min<int>(buckets, static_cast<int>(size));

    struct Bucket {
        bool hasPoint = false;
        std::size_t minIndex = 0;
        std::size_t maxIndex = 0;
    };
    std::vector<Bucket> bucketList(static_cast<std::size_t>(bucketCount));

    for (std::size_t i = 0; i < size; ++i) {
        int index = static_cast<int>((timestamps[i] - base) * bucketCount / range);
        if (index < 0) index = 0;
        if (index >= bucketCount) index = bucketCount - 1;

        Bucket& bucket = bucketList[static_cast<std::size_t>(index)];
        if (!bucket.hasPoint) {
            bucket.hasPoint = true;
            bucket.minIndex = bucket.maxIndex = i;
            continue;
        }
        if (power[i] < power[bucket.minIndex]) bucket.minIndex = i;
        if (power[i] > power[bucket.maxIndex]) bucket.maxIndex = i;
    }

    std::vector<float> packed;
    packed.reserve(static_cast<std::size_t>(bucketCount) * 6);
    for (const Bucket& bucket : bucketList) {
        if (!bucket.hasPoint) continue;
        std::size_t first = bucket.minIndex;
        std::size_t second = bucket.maxIndex;
        if (first > second) std::swap(first, second);

        packed.push_back(static_cast<float>(timestamps[first] - base));
        packed.push_back(power[first]);
        packed.push_back(temperature[first]);
        if (second != first) {
            packed.push_back(static_cast<float>(timestamps[second] - base));
            packed.push_back(power[second]);
            packed.push_back(temperature[second]);
        }
    }
    return packed;
}

}  // namespace ampy::chart

#ifndef AMPY_NATIVE_STANDALONE_TEST

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_zenin_ampy_nativelib_AmpyChartNative_downsample(
        JNIEnv* env, jobject /*thiz*/,
        jlongArray timestamps, jfloatArray power,
        jfloatArray temperature, jint buckets) {
    if (timestamps == nullptr || power == nullptr || temperature == nullptr) return nullptr;

    const jsize count = env->GetArrayLength(timestamps);
    if (count != env->GetArrayLength(power) || count != env->GetArrayLength(temperature)) {
        return nullptr;
    }

    std::vector<std::int64_t> nativeTimestamps(static_cast<std::size_t>(count));
    std::vector<float> nativePower(static_cast<std::size_t>(count));
    std::vector<float> nativeTemperature(static_cast<std::size_t>(count));
    env->GetLongArrayRegion(timestamps, 0, count,
                            reinterpret_cast<jlong*>(nativeTimestamps.data()));
    env->GetFloatArrayRegion(power, 0, count, nativePower.data());
    env->GetFloatArrayRegion(temperature, 0, count, nativeTemperature.data());

    const std::vector<float> packed = ampy::chart::downsampleSeries(
        nativeTimestamps, nativePower, nativeTemperature, buckets);
    if (packed.empty()) return nullptr;

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(packed.size()));
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(packed.size()), packed.data());
    }
    return result;
}

#else

#include <cassert>
#include <iostream>

int main() {
    std::vector<std::int64_t> times(100);
    std::vector<float> power(100);
    std::vector<float> temp(100);
    for (int i = 0; i < 100; ++i) {
        times[i] = i * 1000LL;
        power[i] = static_cast<float>(i == 42 ? 900 : i);
        temp[i] = 30.0f + i / 10.0f;
    }

    const std::vector<float> packed = ampy::chart::downsampleSeries(times, power, temp, 10);
    assert(!packed.empty());
    assert(packed.size() <= 60);

    bool peakKept = false;
    for (std::size_t i = 1; i < packed.size(); i += 3) {
        if (packed[i] == 900.0f) peakKept = true;
    }
    assert(peakKept);

    std::cout << "ampy chart downsample ok\n";
    return 0;
}

#endif
