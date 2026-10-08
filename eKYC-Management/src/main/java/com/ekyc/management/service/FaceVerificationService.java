package com.ekyc.management.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class FaceVerificationService {

    // ============================================================
    // MODEL PATHS
    // ============================================================

    private static final String YUNET_MODEL =
            "src/main/resources/models/face_detection_yunet_2023mar.onnx";

    private static final String SFACE_MODEL =
            "src/main/resources/models/face_recognition_sface_2021dec.onnx";

    // ============================================================
    // YUNET SETTINGS
    // ============================================================

    /*
     * Do NOT use 0.01 here.
     *
     * 0.01 was allowing many low-confidence detections.
     */
    private static final float YUNET_CONFIDENCE_THRESHOLD = 0.90f;

    private static final int MIN_FACE_SIZE = 30;

    // ============================================================
    // SFACE SETTINGS
    // ============================================================

    /*
     * OpenCV reference cosine threshold = 0.363.
     *
     * For this KYC application we use a stricter threshold.
     */
    private static final double SFACE_COSINE_THRESHOLD = 0.363;

    /*
     * OpenCV reference Norm-L2 threshold = 1.128.
     *
     * We use a stricter threshold for this KYC application.
     */
    private static final double SFACE_L2_THRESHOLD = 1.128;

    /*
     * If the two extracted feature vectors are mathematically
     * identical, something is wrong with feature extraction.
     *
     * Do not allow KYC approval in that situation.
     */
    private static final double MIN_FEATURE_DISTANCE = 0.000001;

    /*
     * Feature vector must have meaningful magnitude.
     */
    private static final double MIN_FEATURE_NORM = 0.000001;

    // ============================================================
    // YUNET OUTPUT
    // ============================================================

    /*
     * YuNet detection format:
     *
     * 0  = x
     * 1  = y
     * 2  = width
     * 3  = height
     * 4  = right eye x
     * 5  = right eye y
     * 6  = left eye x
     * 7  = left eye y
     * 8  = nose x
     * 9  = nose y
     * 10 = right mouth x
     * 11 = right mouth y
     * 12 = left mouth x
     * 13 = left mouth y
     * 14 = confidence
     */
    private static final int VALUES_PER_FACE = 15;

    // ============================================================
    // OPENCV OBJECTS
    // ============================================================

    private FaceDetectorYN faceDetector;

    private FaceRecognizerSF faceRecognizer;

    // ============================================================
    // INITIALIZATION
    // ============================================================

    @PostConstruct
    public void initialize() {

        try {

            // ----------------------------------------------------
            // Load OpenCV
            // ----------------------------------------------------

            try {

                nu.pattern.OpenCV.loadLocally();

                System.out.println(
                        "OpenCV loaded using nu.pattern.OpenCV"
                );

            } catch (Throwable ex) {

                System.out.println(
                        "nu.pattern.OpenCV loading failed."
                                + " Trying System.loadLibrary..."
                );

                System.loadLibrary(
                        Core.NATIVE_LIBRARY_NAME
                );

                System.out.println(
                        "OpenCV loaded using System.loadLibrary"
                );
            }

            // ----------------------------------------------------
            // Check model paths
            // ----------------------------------------------------

            Path yunetPath =
                    Paths.get(YUNET_MODEL);

            Path sfacePath =
                    Paths.get(SFACE_MODEL);

            if (!Files.exists(yunetPath)) {

                throw new IOException(
                        "YuNet model not found: "
                                + yunetPath.toAbsolutePath()
                );
            }

            if (!Files.exists(sfacePath)) {

                throw new IOException(
                        "SFace model not found: "
                                + sfacePath.toAbsolutePath()
                );
            }

            System.out.println(
                    "YuNet model size: "
                            + Files.size(yunetPath)
                            + " bytes"
            );

            System.out.println(
                    "SFace model size: "
                            + Files.size(sfacePath)
                            + " bytes"
            );

            // ----------------------------------------------------
            // Create YuNet
            // ----------------------------------------------------

            faceDetector =
                    FaceDetectorYN.create(
                            yunetPath.toString(),
                            "",
                            new Size(320, 320),
                            YUNET_CONFIDENCE_THRESHOLD,
                            0.3f,
                            5000
                    );

            // ----------------------------------------------------
            // Create SFace
            // ----------------------------------------------------

            faceRecognizer =
                    FaceRecognizerSF.create(
                            sfacePath.toString(),
                            ""
                    );

            // ----------------------------------------------------
            // Startup information
            // ----------------------------------------------------

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "FaceVerificationService initialized"
            );

            System.out.println(
                    "YuNet confidence threshold : "
                            + YUNET_CONFIDENCE_THRESHOLD
            );

            System.out.println(
                    "SFace cosine threshold    : "
                            + SFACE_COSINE_THRESHOLD
            );

            System.out.println(
                    "SFace L2 threshold        : "
                            + SFACE_L2_THRESHOLD
            );

            System.out.println(
                    "=============================================="
            );

        } catch (Exception e) {

            System.err.println(
                    "FaceVerificationService initialization failed"
            );

            e.printStackTrace();

            throw new IllegalStateException(
                    "Could not initialize face verification service",
                    e
            );
        }
    }

    // ============================================================
    // MAIN VERIFICATION
    // ============================================================

    public FaceVerificationResult verifyFaces(
            String proofImagePath,
            byte[] liveImageBytes) {

        Mat proofImage = null;
        Mat liveImage = null;

        Mat proofFeature = null;
        Mat liveFeature = null;

        try {

            System.out.println();
            System.out.println(
                    "==============================================="
            );

            System.out.println(
                    "FACE VERIFICATION STARTED"
            );

            System.out.println(
                    "==============================================="
            );

            // ====================================================
            // VALIDATE PROOF
            // ====================================================

            if (proofImagePath == null
                    || proofImagePath.isBlank()) {

                return failure(
                        "INVALID_PROOF_IMAGE_PATH",
                        0.0
                );
            }

            Path proofPath =
                    Paths.get(proofImagePath);

            if (!Files.exists(proofPath)) {

                return failure(
                        "PROOF_IMAGE_NOT_FOUND",
                        0.0
                );
            }

            // ====================================================
            // VALIDATE LIVE IMAGE
            // ====================================================

            if (liveImageBytes == null
                    || liveImageBytes.length == 0) {

                return failure(
                        "INVALID_LIVE_IMAGE",
                        0.0
                );
            }

            System.out.println(
                    "Proof image: "
                            + proofPath.toAbsolutePath()
            );

            // ====================================================
            // READ PROOF IMAGE
            // ====================================================

            proofImage =
                    Imgcodecs.imread(
                            proofPath.toString(),
                            Imgcodecs.IMREAD_COLOR
                    );

            if (proofImage == null
                    || proofImage.empty()) {

                return failure(
                        "PROOF_IMAGE_COULD_NOT_BE_READ",
                        0.0
                );
            }

            System.out.println(
                    "Proof image size: "
                            + proofImage.cols()
                            + "x"
                            + proofImage.rows()
            );

            // ====================================================
            // READ LIVE IMAGE
            // ====================================================

            MatOfByte liveBytes =
                    new MatOfByte(liveImageBytes);

            try {

                liveImage =
                        Imgcodecs.imdecode(
                                liveBytes,
                                Imgcodecs.IMREAD_COLOR
                        );

            } finally {

                liveBytes.release();
            }

            if (liveImage == null
                    || liveImage.empty()) {

                return failure(
                        "LIVE_IMAGE_COULD_NOT_BE_DECODED",
                        0.0
                );
            }

            System.out.println(
                    "Live image size: "
                            + liveImage.cols()
                            + "x"
                            + liveImage.rows()
            );

            System.out.println(
                    "Live image bytes: "
                            + liveImageBytes.length
            );

            // ====================================================
            // STEP 1
            // PROOF FACE DETECTION
            // ====================================================

            System.out.println();
            System.out.println(
                    "-----------------------------------------------"
            );

            System.out.println(
                    "STEP 1: Detecting face in proof image"
            );

            System.out.println(
                    "-----------------------------------------------"
            );

            Mat proofDetections =
                    detectFaces(proofImage);

            if (proofDetections == null
                    || proofDetections.empty()
                    || proofDetections.rows() == 0) {

                System.out.println(
                        "No proof face detected."
                );

                return failure(
                        "NO_FACE_FOUND_IN_PROOF_IMAGE",
                        0.0
                );
            }

            System.out.println(
                    "Final proof detections: "
                            + proofDetections.rows()
            );

            // ====================================================
            // SELECT PROOF FACE
            // ====================================================

            FaceBox proofBox =
                    selectBestFace(
                            proofDetections,
                            proofImage.cols(),
                            proofImage.rows()
                    );

            proofDetections.release();

            if (proofBox == null) {

                return failure(
                        "NO_VALID_FACE_IN_PROOF_IMAGE",
                        0.0
                );
            }

            System.out.println(
                    "Selected proof face:"
                            + " x=" + proofBox.x
                            + " y=" + proofBox.y
                            + " width=" + proofBox.width
                            + " height=" + proofBox.height
                            + " confidence=" + proofBox.confidence
            );

            // ====================================================
            // STEP 2
            // LIVE FACE DETECTION
            // ====================================================

            System.out.println();
            System.out.println(
                    "-----------------------------------------------"
            );

            System.out.println(
                    "STEP 2: Detecting face in live image"
            );

            System.out.println(
                    "-----------------------------------------------"
            );

            Mat liveDetections =
                    detectFaces(liveImage);

            if (liveDetections == null
                    || liveDetections.empty()
                    || liveDetections.rows() == 0) {

                return failure(
                        "NO_FACE_FOUND_IN_LIVE_IMAGE",
                        0.0
                );
            }

            System.out.println(
                    "Live detections: "
                            + liveDetections.rows()
            );

            /*
             * Exactly one high-confidence face must exist
             * in the live image.
             */

            if (liveDetections.rows() != 1) {

                int count =
                        liveDetections.rows();

                liveDetections.release();

                return failure(
                        "LIVE_IMAGE_MUST_CONTAIN_EXACTLY_ONE_FACE_FOUND_"
                                + count,
                        0.0
                );
            }

            FaceBox liveBox =
                    selectBestFace(
                            liveDetections,
                            liveImage.cols(),
                            liveImage.rows()
                    );

            liveDetections.release();

            if (liveBox == null) {

                return failure(
                        "NO_VALID_FACE_IN_LIVE_IMAGE",
                        0.0
                );
            }

            System.out.println(
                    "Selected live face:"
                            + " x=" + liveBox.x
                            + " y=" + liveBox.y
                            + " width=" + liveBox.width
                            + " height=" + liveBox.height
                            + " confidence=" + liveBox.confidence
            );

            // ====================================================
            // STEP 3
            // ALIGN FACES
            // ====================================================

            System.out.println();
            System.out.println(
                    "-----------------------------------------------"
            );

            System.out.println(
                    "STEP 3: Aligning faces using SFace"
            );

            System.out.println(
                    "-----------------------------------------------"
            );

            Mat alignedProof =
                    new Mat();

            Mat alignedLive =
                    new Mat();

            try {

                Mat proofDetectionRow =
                        createDetectionRow(
                                proofBox
                        );

                Mat liveDetectionRow =
                        createDetectionRow(
                                liveBox
                        );

                try {

                    faceRecognizer.alignCrop(
                            proofImage,
                            proofDetectionRow,
                            alignedProof
                    );

                    faceRecognizer.alignCrop(
                            liveImage,
                            liveDetectionRow,
                            alignedLive
                    );

                } finally {

                    proofDetectionRow.release();
                    liveDetectionRow.release();
                }

                if (alignedProof.empty()) {

                    return failure(
                            "PROOF_FACE_ALIGNMENT_FAILED",
                            0.0
                    );
                }

                if (alignedLive.empty()) {

                    return failure(
                            "LIVE_FACE_ALIGNMENT_FAILED",
                            0.0
                    );
                }

                System.out.println(
                        "Aligned proof: "
                                + alignedProof.cols()
                                + "x"
                                + alignedProof.rows()
                );

                System.out.println(
                        "Aligned live: "
                                + alignedLive.cols()
                                + "x"
                                + alignedLive.rows()
                );

                // =================================================
                // STEP 4
                // EXTRACT FEATURES
                // =================================================

                System.out.println();
                System.out.println(
                        "-----------------------------------------------"
                );

                System.out.println(
                        "STEP 4: Extracting SFace features"
                );

                System.out.println(
                        "-----------------------------------------------"
                );

                /*
                 * IMPORTANT FIX:
                 *
                 * Do NOT directly use the same native output
                 * Mat for both feature extraction operations.
                 *
                 * OpenCV's native SFace implementation can reuse
                 * its output buffer. Therefore:
                 *
                 * 1. Extract proof feature.
                 * 2. Clone it immediately.
                 * 3. Extract live feature separately.
                 *
                 * This prevents the proof feature from being
                 * overwritten when the live feature is generated.
                 */

                Mat proofFeatureRaw =
                        new Mat();

                try {

                    faceRecognizer.feature(
                            alignedProof,
                            proofFeatureRaw
                    );

                    if (proofFeatureRaw.empty()) {

                        return failure(
                                "PROOF_FEATURE_EXTRACTION_FAILED",
                                0.0
                        );
                    }

                    /*
                     * CRITICAL FIX:
                     *
                     * Make an independent native copy.
                     */
                    proofFeature =
                            proofFeatureRaw.clone();

                } finally {

                    proofFeatureRaw.release();
                }

                /*
                 * Extract the live feature into its own Mat.
                 */
                Mat liveFeatureRaw =
                        new Mat();

                try {

                    faceRecognizer.feature(
                            alignedLive,
                            liveFeatureRaw
                    );

                    if (liveFeatureRaw.empty()) {

                        return failure(
                                "LIVE_FEATURE_EXTRACTION_FAILED",
                                0.0
                        );
                    }

                    /*
                     * Also clone the live feature so the Java
                     * object owns an independent native matrix.
                     */
                    liveFeature =
                            liveFeatureRaw.clone();

                } finally {

                    liveFeatureRaw.release();
                }

                // -------------------------------------------------
                // Verify feature matrices
                // -------------------------------------------------

                if (proofFeature == null
                        || proofFeature.empty()) {

                    return failure(
                            "PROOF_FEATURE_EXTRACTION_FAILED",
                            0.0
                    );
                }

                if (liveFeature == null
                        || liveFeature.empty()) {

                    return failure(
                            "LIVE_FEATURE_EXTRACTION_FAILED",
                            0.0
                    );
                }

                System.out.println(
                        "Proof feature:"
                                + " rows="
                                + proofFeature.rows()
                                + " cols="
                                + proofFeature.cols()
                                + " type="
                                + proofFeature.type()
                );

                System.out.println(
                        "Live feature:"
                                + " rows="
                                + liveFeature.rows()
                                + " cols="
                                + liveFeature.cols()
                                + " type="
                                + liveFeature.type()
                );

                // =================================================
                // FEATURE VALIDATION
                // =================================================

                double proofNorm =
                        calculateFeatureNorm(
                                proofFeature
                        );

                double liveNorm =
                        calculateFeatureNorm(
                                liveFeature
                        );

                System.out.println(
                        "Proof feature norm: "
                                + proofNorm
                );

                System.out.println(
                        "Live feature norm: "
                                + liveNorm
                );

                if (!Double.isFinite(proofNorm)
                        || !Double.isFinite(liveNorm)) {

                    return failure(
                            "INVALID_FEATURE_VECTOR",
                            0.0
                    );
                }

                if (proofNorm < MIN_FEATURE_NORM) {

                    return failure(
                            "PROOF_FEATURE_VECTOR_INVALID",
                            0.0
                    );
                }

                if (liveNorm < MIN_FEATURE_NORM) {

                    return failure(
                            "LIVE_FEATURE_VECTOR_INVALID",
                            0.0
                    );
                }

                // =================================================
                // OPTIONAL FEATURE DEBUGGING
                // =================================================

                printFeaturePreview(
                        "Proof feature",
                        proofFeature
                );

                printFeaturePreview(
                        "Live feature",
                        liveFeature
                );

                // =================================================
                // STEP 5
                // FACE COMPARISON
                // =================================================

                System.out.println();
                System.out.println(
                        "-----------------------------------------------"
                );

                System.out.println(
                        "STEP 5: Comparing faces"
                );

                System.out.println(
                        "-----------------------------------------------"
                );

                // -------------------------------------------------
                // COSINE
                // -------------------------------------------------

                double cosineSimilarity =
                        faceRecognizer.match(
                                proofFeature,
                                liveFeature,
                                FaceRecognizerSF.FR_COSINE
                        );

                // -------------------------------------------------
                // NORM L2
                // -------------------------------------------------

                double l2Distance =
                        faceRecognizer.match(
                                proofFeature,
                                liveFeature,
                                FaceRecognizerSF.FR_NORM_L2
                        );

                // -------------------------------------------------
                // MANUAL FEATURE DISTANCE
                // -------------------------------------------------

                double manualL2 =
                        calculateFeatureDistance(
                                proofFeature,
                                liveFeature
                        );

                System.out.println(
                        "SFace cosine similarity: "
                                + cosineSimilarity
                );

                System.out.println(
                        "SFace Norm-L2 distance: "
                                + l2Distance
                );

                System.out.println(
                        "Manual feature L2 distance: "
                                + manualL2
                );

                // =================================================
                // VALIDATE SCORES
                // =================================================

                if (!Double.isFinite(cosineSimilarity)
                        || !Double.isFinite(l2Distance)
                        || !Double.isFinite(manualL2)) {

                    return failure(
                            "INVALID_FACE_SIMILARITY",
                            0.0
                    );
                }

                /*
                 * Cosine must be in the normal range.
                 */

                if (cosineSimilarity < -1.0
                        || cosineSimilarity > 1.0) {

                    return failure(
                            "INVALID_COSINE_SCORE",
                            0.0
                    );
                }

                if (l2Distance < 0.0) {

                    return failure(
                            "INVALID_L2_SCORE",
                            cosineSimilarity
                    );
                }

                // =================================================
                // DETECT DEGENERATE FEATURE OUTPUT
                // =================================================

                /*
                 * If the two feature vectors are essentially
                 * identical, something is wrong with feature
                 * extraction or the inputs.
                 *
                 * Do NOT allow KYC to be updated in this situation.
                 */

                if (manualL2 < MIN_FEATURE_DISTANCE) {

                    System.err.println(
                            "WARNING: Proof and live feature vectors "
                                    + "are almost identical."
                    );

                    System.err.println(
                            "Verification rejected to prevent "
                                    + "false KYC approval."
                    );

                    return failure(
                            "FEATURE_VECTORS_SUSPICIOUSLY_IDENTICAL",
                            cosineSimilarity
                    );
                }

                // =================================================
                // FINAL MATCH DECISION
                // =================================================

                /*
                 * Require BOTH:
                 *
                 * 1. Cosine similarity >= 0.50
                 * 2. Norm-L2 distance <= 1.00
                 */

                boolean cosinePassed =
                        cosineSimilarity
                                >= SFACE_COSINE_THRESHOLD;

                boolean l2Passed =
                        l2Distance
                                <= SFACE_L2_THRESHOLD;

                System.out.println(
                        "Cosine passed: "
                                + cosinePassed
                );

                System.out.println(
                        "L2 passed: "
                                + l2Passed
                );

                // =================================================
                // MATCH
                // =================================================

                if (cosinePassed
                        && l2Passed) {

                    System.out.println(
                            "=============================================="
                    );

                    System.out.println(
                            "FACE MATCH: VERIFIED"
                    );

                    System.out.println(
                            "Cosine = "
                                    + cosineSimilarity
                                    + " >= "
                                    + SFACE_COSINE_THRESHOLD
                    );

                    System.out.println(
                            "L2 = "
                                    + l2Distance
                                    + " <= "
                                    + SFACE_L2_THRESHOLD
                    );

                    System.out.println(
                            "=============================================="
                    );

                    return new FaceVerificationResult(
                            true,
                            cosineSimilarity,
                            "VERIFIED"
                    );
                }

                // =================================================
                // MISMATCH
                // =================================================

                System.out.println(
                        "=============================================="
                );

                System.out.println(
                        "FACE MATCH: FAILED"
                );

                System.out.println(
                        "Cosine = "
                                + cosineSimilarity
                                + " >= "
                                + SFACE_COSINE_THRESHOLD
                                + " ? "
                                + cosinePassed
                );

                System.out.println(
                        "L2 = "
                                + l2Distance
                                + " <= "
                                + SFACE_L2_THRESHOLD
                                + " ? "
                                + l2Passed
                );

                System.out.println(
                        "=============================================="
                );

                return new FaceVerificationResult(
                        false,
                        cosineSimilarity,
                        "FACE_MISMATCH"
                );

            } finally {

                alignedProof.release();
                alignedLive.release();
            }

        } catch (Exception e) {

            System.err.println(
                    "Exception during face verification:"
            );

            e.printStackTrace();

            return failure(
                    "FACE_VERIFICATION_ERROR",
                    0.0
            );

        } finally {

            release(proofImage);
            release(liveImage);
            release(proofFeature);
            release(liveFeature);
        }
    }

    // ============================================================
    // YUNET DETECTION
    // ============================================================

    private Mat detectFaces(Mat image) {

        if (image == null
                || image.empty()) {

            return new Mat();
        }

        System.out.println(
                "Proof/live image dimensions: "
                        + image.cols()
                        + "x"
                        + image.rows()
        );

        /*
         * YuNet must receive actual image dimensions.
         */

        faceDetector.setInputSize(
                new Size(
                        image.cols(),
                        image.rows()
                )
        );

        Mat rawDetections =
                new Mat();

        faceDetector.detect(
                image,
                rawDetections
        );

        System.out.println(
                "YuNet raw matrix: "
                        + rawDetections.rows()
                        + " rows x "
                        + rawDetections.cols()
                        + " cols"
        );

        System.out.println(
                "YuNet raw total values: "
                        + rawDetections.total()
        );

        System.out.println(
                "YuNet channels: "
                        + rawDetections.channels()
        );

        if (rawDetections.empty()) {

            rawDetections.release();

            return new Mat();
        }

        Mat normalized =
                normalizeDetections(
                        rawDetections
                );

        rawDetections.release();

        if (normalized.empty()) {

            System.out.println(
                    "YuNet normalization failed."
            );

            return new Mat();
        }

        System.out.println(
                "Normalized detections: "
                        + normalized.rows()
                        + " x "
                        + normalized.cols()
        );

        Mat validDetections =
                new Mat(
                        0,
                        VALUES_PER_FACE,
                        CvType.CV_32FC1
                );

        int usableCount = 0;

        for (int i = 0;
                i < normalized.rows();
                i++) {

            float[] values =
                    new float[VALUES_PER_FACE];

            int readCount =
                    normalized.get(
                            i,
                            0,
                            values
                    );

            if (readCount <= 0) {

                System.out.println(
                        "Detection "
                                + i
                                + " could not be read."
                );

                continue;
            }

            double x =
                    values[0];

            double y =
                    values[1];

            double width =
                    values[2];

            double height =
                    values[3];

            double confidence =
                    values[14];

            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(width)
                    || !Double.isFinite(height)
                    || !Double.isFinite(confidence)) {

                continue;
            }

            if (width <= 0
                    || height <= 0) {

                continue;
            }

            if (confidence
                    < YUNET_CONFIDENCE_THRESHOLD) {

                System.out.println(
                        "Detection "
                                + i
                                + " rejected:"
                                + " confidence="
                                + confidence
                );

                continue;
            }

            if (width < MIN_FACE_SIZE
                    || height < MIN_FACE_SIZE) {

                continue;
            }

            System.out.println(
                    "Detection "
                            + i
                            + " VALID:"
                            + " x=" + x
                            + " y=" + y
                            + " w=" + width
                            + " h=" + height
                            + " confidence="
                            + confidence
            );

            Mat oneRow =
                    new Mat(
                            1,
                            VALUES_PER_FACE,
                            CvType.CV_32FC1
                    );

            oneRow.put(
                    0,
                    0,
                    values
            );

            validDetections.push_back(
                    oneRow
            );

            oneRow.release();

            usableCount++;
        }

        normalized.release();

        System.out.println(
                "YuNet usable detections: "
                        + usableCount
        );

        return validDetections;
    }

    // ============================================================
    // NORMALIZE YUNET MATRIX
    // ============================================================

    private Mat normalizeDetections(
            Mat raw) {

        if (raw == null
                || raw.empty()) {

            return new Mat();
        }

        /*
         * Standard YuNet output:
         *
         * N x 15
         */

        if (raw.cols()
                == VALUES_PER_FACE) {

            return raw.clone();
        }

        /*
         * Also handle:
         *
         * 1 x (N * 15)
         */

        long totalValues =
                raw.total()
                        * raw.channels();

        if (totalValues
                % VALUES_PER_FACE != 0) {

            System.out.println(
                    "Cannot normalize YuNet output."
                            + " total="
                            + totalValues
            );

            return new Mat();
        }

        int numberOfFaces =
                (int) (
                        totalValues
                                / VALUES_PER_FACE
                );

        try {

            Mat reshaped =
                    raw.reshape(
                            1,
                            numberOfFaces
                    );

            return reshaped.clone();

        } catch (Exception e) {

            System.err.println(
                    "YuNet reshape failed."
            );

            e.printStackTrace();

            return new Mat();
        }
    }

    // ============================================================
    // SELECT BEST FACE
    // ============================================================

    private FaceBox selectBestFace(
            Mat detections,
            int imageWidth,
            int imageHeight) {

        if (detections == null
                || detections.empty()
                || detections.cols()
                < VALUES_PER_FACE) {

            return null;
        }

        FaceBox best =
                null;

        double bestScore =
                -1;

        for (int i = 0;
                i < detections.rows();
                i++) {

            float[] values =
                    new float[VALUES_PER_FACE];

            int readCount =
                    detections.get(
                            i,
                            0,
                            values
                    );

            if (readCount <= 0) {

                continue;
            }

            double x =
                    values[0];

            double y =
                    values[1];

            double width =
                    values[2];

            double height =
                    values[3];

            double confidence =
                    values[14];

            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(width)
                    || !Double.isFinite(height)
                    || !Double.isFinite(confidence)) {

                continue;
            }

            if (confidence
                    < YUNET_CONFIDENCE_THRESHOLD) {

                continue;
            }

            if (width <= 0
                    || height <= 0) {

                continue;
            }

            int left =
                    clamp(
                            (int) Math.round(x),
                            0,
                            imageWidth - 1
                    );

            int top =
                    clamp(
                            (int) Math.round(y),
                            0,
                            imageHeight - 1
                    );

            int right =
                    clamp(
                            (int) Math.round(
                                    x + width
                            ),
                            1,
                            imageWidth
                    );

            int bottom =
                    clamp(
                            (int) Math.round(
                                    y + height
                            ),
                            1,
                            imageHeight
                    );

            int finalWidth =
                    right - left;

            int finalHeight =
                    bottom - top;

            if (finalWidth
                    < MIN_FACE_SIZE
                    || finalHeight
                    < MIN_FACE_SIZE) {

                continue;
            }

            double area =
                    (double) finalWidth
                            * finalHeight;

            /*
             * Confidence first,
             * then face size.
             */

            double score =
                    confidence * 1_000_000.0
                            + area;

            if (score > bestScore) {

                bestScore =
                        score;

                best =
                        new FaceBox(
                                left,
                                top,
                                finalWidth,
                                finalHeight,
                                confidence,
                                values
                        );
            }
        }

        return best;
    }

    // ============================================================
    // CREATE DETECTION ROW
    // ============================================================

    private Mat createDetectionRow(
            FaceBox box) {

        Mat row =
                new Mat(
                        1,
                        VALUES_PER_FACE,
                        CvType.CV_32FC1
                );

        row.put(
                0,
                0,
                box.values
        );

        return row;
    }

    // ============================================================
    // FEATURE NORM
    // ============================================================

    private double calculateFeatureNorm(
            Mat feature) {

        if (feature == null
                || feature.empty()) {

            return 0.0;
        }

        int total =
                (int) (
                        feature.total()
                                * feature.channels()
                );

        if (total <= 0) {

            return 0.0;
        }

        float[] values =
                new float[total];

        int read =
                feature.get(
                        0,
                        0,
                        values
                );

        if (read <= 0) {

            return 0.0;
        }

        double sum =
                0.0;

        for (int i = 0;
                i < values.length;
                i++) {

            double value =
                    values[i];

            if (!Double.isFinite(value)) {

                return Double.NaN;
            }

            sum +=
                    value * value;
        }

        return Math.sqrt(sum);
    }

    // ============================================================
    // MANUAL FEATURE L2 DISTANCE
    // ============================================================

    private double calculateFeatureDistance(
            Mat feature1,
            Mat feature2) {

        if (feature1 == null
                || feature2 == null
                || feature1.empty()
                || feature2.empty()) {

            return Double.NaN;
        }

        int total1 =
                (int) (
                        feature1.total()
                                * feature1.channels()
                );

        int total2 =
                (int) (
                        feature2.total()
                                * feature2.channels()
                );

        if (total1 <= 0
                || total2 <= 0
                || total1 != total2) {

            return Double.NaN;
        }

        float[] values1 =
                new float[total1];

        float[] values2 =
                new float[total2];

        int read1 =
                feature1.get(
                        0,
                        0,
                        values1
                );

        int read2 =
                feature2.get(
                        0,
                        0,
                        values2
                );

        if (read1 <= 0
                || read2 <= 0) {

            return Double.NaN;
        }

        double sum =
                0.0;

        for (int i = 0;
                i < total1;
                i++) {

            double a =
                    values1[i];

            double b =
                    values2[i];

            if (!Double.isFinite(a)
                    || !Double.isFinite(b)) {

                return Double.NaN;
            }

            double difference =
                    a - b;

            sum +=
                    difference
                            * difference;
        }

        return Math.sqrt(sum);
    }

    // ============================================================
    // FEATURE PREVIEW
    // ============================================================

    private void printFeaturePreview(
            String name,
            Mat feature) {

        if (feature == null
                || feature.empty()) {

            System.out.println(
                    name
                            + " preview: EMPTY"
            );

            return;
        }

        int total =
                (int) (
                        feature.total()
                                * feature.channels()
                );

        if (total <= 0) {

            return;
        }

        int previewSize =
                Math.min(
                        5,
                        total
                );

        float[] values =
                new float[total];

        int read =
                feature.get(
                        0,
                        0,
                        values
                );

        if (read <= 0) {

            System.out.println(
                    name
                            + " preview: unable to read"
            );

            return;
        }

        StringBuilder builder =
                new StringBuilder();

        builder.append(name);
        builder.append(" first values: ");

        for (int i = 0;
                i < previewSize;
                i++) {

            if (i > 0) {

                builder.append(", ");
            }

            builder.append(
                    values[i]
            );
        }

        System.out.println(
                builder.toString()
        );
    }

    // ============================================================
    // CLAMP
    // ============================================================

    private int clamp(
            int value,
            int min,
            int max) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    // ============================================================
    // FAILURE RESULT
    // ============================================================

    private FaceVerificationResult failure(
            String status,
            double similarity) {

        System.out.println(
                "Face verification failed: "
                        + status
        );

        return new FaceVerificationResult(
                false,
                similarity,
                status
        );
    }

    // ============================================================
    // RELEASE
    // ============================================================

    private void release(Mat mat) {

        if (mat != null) {

            try {

                mat.release();

            } catch (Exception ignored) {

                // Ignore native cleanup errors
            }
        }
    }

    // ============================================================
    // FACE BOX
    // ============================================================

    private static class FaceBox {

        int x;

        int y;

        int width;

        int height;

        double confidence;

        float[] values;

        FaceBox(
                int x,
                int y,
                int width,
                int height,
                double confidence,
                float[] values) {

            this.x =
                    x;

            this.y =
                    y;

            this.width =
                    width;

            this.height =
                    height;

            this.confidence =
                    confidence;

            this.values =
                    values.clone();
        }
    }

    // ============================================================
    // RESULT CLASS
    // ============================================================

    public static class FaceVerificationResult {

        private final boolean matched;

        private final double similarity;

        private final String status;

        public FaceVerificationResult(
                boolean matched,
                double similarity,
                String status) {

            this.matched =
                    matched;

            this.similarity =
                    similarity;

            this.status =
                    status;
        }

        public boolean isMatched() {

            return matched;
        }

        public double getSimilarity() {

            return similarity;
        }

        public String getStatus() {

            return status;
        }
    }
}