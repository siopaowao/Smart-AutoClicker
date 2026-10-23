
/*
 * Copyright (C) 2026 Kevin Buzeau
 * Licensed under the GNU General Public License v3.0 or later.
 */

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>

#include "color_matcher.hpp"
#include "../../../logs/log.h"
#include "../../../utils/roi.h"

using namespace smartautoclicker;

void ColorMatcher::reset() {
    currentMatchingResult.reset();
}

ColorMatchingResult *ColorMatcher::getMatchingResults() {
    return &currentMatchingResult;
}

bool ColorMatcher::isRoiValidForMatching(
        const cv::Rect& screenRoi,
        const cv::Rect& roi) {
    if (roi.width <= 0 || roi.height <= 0 ||
        !isRoiContainsOrEquals(screenRoi, roi)) {
        LOGD("Detector",
             "Invalid color detection area: x=%d y=%d w=%d h=%d",
             roi.x, roi.y, roi.width, roi.height);
        return false;
    }

    return true;
}

void ColorMatcher::matchColor(
        const ScreenImage& screenImage,
        const cv::Scalar& conditionColor,
        const cv::Rect& detectionArea,
        int threshold) {

    // Extract the selected rectangular region.
    cv::Mat cropped = screenImage.cropColor(detectionArea);

    if (cropped.empty()) {
        LOGE("ColorMatcher", "Color detection area is empty.");
        return;
    }

    // Search for at least one matching pixel.
    // OpenCV channel order and conditionColor must remain consistent
    // with the existing project color conversion.
    const int tolerance = threshold * 255 / 100;

    for (int y = 0; y < cropped.rows; ++y) {
        for (int x = 0; x < cropped.cols; ++x) {
            const cv::Vec3b pixel = cropped.at<cv::Vec3b>(y, x);

            int totalDifference = 0;

            for (int channel = 0; channel < 3; ++channel) {
                totalDifference += std::abs(
                    static_cast<int>(pixel[channel]) -
                    static_cast<int>(conditionColor[channel]));
            }

            // Average absolute RGB-channel difference.
            if (totalDifference <= tolerance * 3) {
                currentMatchingResult.updateResults(
                    detectionArea, 0.0);
                currentMatchingResult.markResultAsDetected();
                return;
            }
        }
    }

    // No matching pixel found.
    currentMatchingResult.updateResults(
        detectionArea, 1.0);
}
