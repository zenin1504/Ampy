#include <algorithm>
#include <cmath>
#include <cstdint>
#include <deque>
#include <new>
#include <utility>
#include <vector>

#ifndef AMPY_NATIVE_STANDALONE_TEST
#include <jni.h>
#endif

namespace ampy::stats {

class RollingPowerStats {
public:
    struct Summary {
        double sum;
        double average;
        float min;
        float max;
        std::size_t count;
    };

    explicit RollingPowerStats(std::size_t capacity)
        : capacity_(std::max<std::size_t>(1, capacity)),
          values_(capacity_, 0.0f) {}

    Summary push(float value) {
        const std::uint64_t sequence = sequence_++;
        const std::size_t slot = static_cast<std::size_t>(sequence % capacity_);

        if (count_ < capacity_) {
            ++count_;
        } else {
            sum_ -= values_[slot];
        }
        values_[slot] = value;
        sum_ += static_cast<double>(value);

        while (!minDeque_.empty() && minDeque_.back().second >= value) {
            minDeque_.pop_back();
        }
        minDeque_.emplace_back(sequence, value);
        expire(minDeque_, sequence);

        while (!maxDeque_.empty() && maxDeque_.back().second <= value) {
            maxDeque_.pop_back();
        }
        maxDeque_.emplace_back(sequence, value);
        expire(maxDeque_, sequence);

        return Summary{
            sum_,
            sum_ / static_cast<double>(count_),
            minDeque_.front().second,
            maxDeque_.front().second,
            count_
        };
    }

    void reset() {
        std::fill(values_.begin(), values_.end(), 0.0f);
        minDeque_.clear();
        maxDeque_.clear();
        count_ = 0;
        sum_ = 0.0;
        sequence_ = 0;
    }

private:
    using IndexedValue = std::pair<std::uint64_t, float>;

    void expire(std::deque<IndexedValue>& deque, std::uint64_t sequence) const {
        while (!deque.empty() && deque.front().first + capacity_ <= sequence) {
            deque.pop_front();
        }
    }

    std::size_t capacity_;
    std::vector<float> values_;
    std::size_t count_ = 0;
    double sum_ = 0.0;
    std::uint64_t sequence_ = 0;
    std::deque<IndexedValue> minDeque_;
    std::deque<IndexedValue> maxDeque_;
};

}  // namespace ampy::stats

#ifndef AMPY_NATIVE_STANDALONE_TEST

static ampy::stats::RollingPowerStats* fromHandle(jlong handle) {
    return reinterpret_cast<ampy::stats::RollingPowerStats*>(handle);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_zenin_ampy_nativelib_AmpyStatsNative_create(
        JNIEnv* /*env*/, jobject /*thiz*/, jint capacity) {
    const std::size_t safeCapacity = capacity > 0
        ? static_cast<std::size_t>(capacity)
        : 1;
    return reinterpret_cast<jlong>(new (std::nothrow) ampy::stats::RollingPowerStats(safeCapacity));
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_zenin_ampy_nativelib_AmpyStatsNative_push(
        JNIEnv* env, jobject /*thiz*/, jlong handle, jfloat value) {
    ampy::stats::RollingPowerStats* stats = fromHandle(handle);
    if (stats == nullptr) return nullptr;

    const ampy::stats::RollingPowerStats::Summary summary = stats->push(value);
    const float packed[5] = {
        static_cast<float>(summary.sum),
        static_cast<float>(summary.average),
        summary.min,
        summary.max,
        static_cast<float>(summary.count)
    };
    jfloatArray result = env->NewFloatArray(5);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 5, packed);
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_zenin_ampy_nativelib_AmpyStatsNative_reset(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    ampy::stats::RollingPowerStats* stats = fromHandle(handle);
    if (stats != nullptr) stats->reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_zenin_ampy_nativelib_AmpyStatsNative_destroy(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    delete fromHandle(handle);
}

#else

#include <cassert>
#include <iostream>

int main() {
    ampy::stats::RollingPowerStats stats(3);

    auto s = stats.push(1.0f);
    assert(s.count == 1);
    assert(std::fabs(s.average - 1.0) < 0.001);
    assert(s.min == 1.0f && s.max == 1.0f);

    s = stats.push(5.0f);
    assert(s.count == 2);
    assert(std::fabs(s.average - 3.0) < 0.001);
    assert(s.min == 1.0f && s.max == 5.0f);

    s = stats.push(2.0f);
    assert(s.count == 3);
    assert(std::fabs(s.average - 8.0 / 3.0) < 0.001);
    assert(s.min == 1.0f && s.max == 5.0f);

    s = stats.push(9.0f);
    assert(s.count == 3);
    assert(std::fabs(s.average - 16.0 / 3.0) < 0.001);
    assert(s.min == 2.0f && s.max == 9.0f);

    s = stats.push(0.0f);
    assert(s.count == 3);
    assert(std::fabs(s.average - 11.0 / 3.0) < 0.001);
    assert(s.min == 0.0f && s.max == 9.0f);

    stats.reset();
    s = stats.push(7.0f);
    assert(s.count == 1);
    assert(s.min == 7.0f && s.max == 7.0f);

    std::cout << "ampy rolling power stats ok\n";
    return 0;
}

#endif
