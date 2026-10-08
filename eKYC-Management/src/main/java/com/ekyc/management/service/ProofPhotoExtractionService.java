package com.ekyc.management.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.springframework.stereotype.Service;

@Service
public class ProofPhotoExtractionService {

    // ============================================================
    // PATHS
    // ============================================================

    private static final String OUTPUT_DIRECTORY =
            "uploads/proof-faces";

    private static final String YUNET_MODEL =
            "src/main/resources/models/face_detection_yunet_2023mar.onnx";

    // ============================================================
    // SETTINGS
    // ============================================================

    /*
     * High PDF resolution.
     *
     * This is important because the Aadhaar photograph is
     * relatively small compared with the complete PDF page.
     */
    private static final float PDF_DPI = 400f;

    /*
     * YuNet internal threshold.
     *
     * Keep this low because the portrait on an ID document
     * can be smaller/blurrier than a normal camera image.
     */
    private static final float YUNET_THRESHOLD = 0.05f;

    /*
     * Minimum confidence that we accept ourselves.
     */
    private static final double MIN_FACE_CONFIDENCE = 0.05;

    /*
     * Maximum generated proof image dimension.
     *
     * CHANGED ONLY THIS VALUE:
     * 800 -> 1600
     *
     * This preserves more resolution from the 400 DPI
     * PDF portrait before FaceVerificationService processes it.
     */
    private static final int MAX_OUTPUT_SIZE = 1600;

    private final FaceDetectorYN faceDetector;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public ProofPhotoExtractionService() {

        try {

            nu.pattern.OpenCV.loadLocally();

        } catch (Exception e) {

            try {

                System.loadLibrary(
                        org.opencv.core.Core.NATIVE_LIBRARY_NAME
                );

            } catch (Exception ex) {

                throw new IllegalStateException(
                        "Unable to load OpenCV.",
                        ex
                );
            }
        }

        Path modelPath =
                Paths.get(YUNET_MODEL);

        if (!Files.exists(modelPath)) {

            throw new IllegalStateException(
                    "YuNet model not found: "
                            + modelPath.toAbsolutePath()
            );
        }

        faceDetector =
                FaceDetectorYN.create(
                        modelPath.toAbsolutePath().toString(),
                        "",
                        new Size(320, 320),
                        YUNET_THRESHOLD,
                        0.3f,
                        5000
                );

        System.out.println();
        System.out.println(
                "================================================"
        );
        System.out.println(
                "ProofPhotoExtractionService started"
        );
        System.out.println(
                "YuNet model: "
                        + modelPath.toAbsolutePath()
        );
        System.out.println(
                "PDF DPI: " + PDF_DPI
        );
        System.out.println(
                "================================================"
        );
        System.out.println();
    }

    // ============================================================
    // MAIN METHOD
    // ============================================================

    public String extractFace(
            String proofPath,
            String customerId,
            Long requestId)
            throws IOException {

        if (proofPath == null
                || proofPath.isBlank()) {

            throw new IllegalArgumentException(
                    "Proof file path is missing."
            );
        }

        Path input =
                Paths.get(proofPath)
                        .toAbsolutePath()
                        .normalize();

        if (!Files.exists(input)) {

            throw new IOException(
                    "Proof file does not exist: "
                            + input
            );
        }

        Files.createDirectories(
                Paths.get(OUTPUT_DIRECTORY)
        );

        String extension =
                getExtension(
                        input.getFileName().toString()
                );

        System.out.println();
        System.out.println(
                "================================================"
        );
        System.out.println(
                "PROOF EXTRACTION STARTED"
        );
        System.out.println(
                "Customer ID : " + customerId
        );
        System.out.println(
                "Request ID  : " + requestId
        );
        System.out.println(
                "Proof       : " + input
        );
        System.out.println(
                "Format      : " + extension
        );
        System.out.println(
                "================================================"
        );

        if ("pdf".equals(extension)) {

            return extractFromPdf(
                    input,
                    customerId,
                    requestId
            );
        }

        if ("jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)) {

            return extractFromImage(
                    input,
                    customerId,
                    requestId
            );
        }

        throw new IllegalArgumentException(
                "Unsupported proof format. "
                        + "Please upload PDF, JPG, JPEG or PNG."
        );
    }

    // ============================================================
    // PDF
    // ============================================================

    private String extractFromPdf(
            Path pdfPath,
            String customerId,
            Long requestId)
            throws IOException {

        try (PDDocument document =
                     Loader.loadPDF(pdfPath.toFile())) {

            int pageCount =
                    document.getNumberOfPages();

            if (pageCount == 0) {

                throw new IllegalArgumentException(
                        "The uploaded PDF contains no pages."
                );
            }

            System.out.println(
                    "PDF pages: " + pageCount
            );

            PDFRenderer renderer =
                    new PDFRenderer(document);

            /*
             * Process every page.
             */
            for (int page = 0;
                 page < pageCount;
                 page++) {

                System.out.println();
                System.out.println(
                        "Processing PDF page "
                                + (page + 1)
                                + " / "
                                + pageCount
                );

                BufferedImage bufferedImage =
                        renderer.renderImageWithDPI(
                                page,
                                PDF_DPI,
                                ImageType.RGB
                        );

                Path temp =
                        Files.createTempFile(
                                "ekyc-page-",
                                ".png"
                        );

                try {

                    ImageIO.write(
                            bufferedImage,
                            "PNG",
                            temp.toFile()
                    );

                    Mat pageImage =
                            Imgcodecs.imread(
                                    temp.toString(),
                                    Imgcodecs.IMREAD_COLOR
                            );

                    if (pageImage.empty()) {

                        pageImage.release();

                        continue;
                    }

                    try {

                        System.out.println(
                                "Rendered page size: "
                                        + pageImage.cols()
                                        + " x "
                                        + pageImage.rows()
                        );

                        /*
                         * =================================================
                         * FIRST:
                         * Aadhaar portrait region.
                         * =================================================
                         *
                         * Your uploaded Aadhaar has the portrait on
                         * the left side.
                         */
                        String result =
                                extractAadhaarPortraitRegion(
                                        pageImage,
                                        customerId,
                                        requestId
                                );

                        if (result != null) {

                            return result;
                        }

                        /*
                         * =================================================
                         * SECOND:
                         * Generic full-page face detection.
                         * =================================================
                         */
                        FaceCandidate candidate =
                                findBestFace(
                                        pageImage
                                );

                        if (candidate != null) {

                            System.out.println(
                                    "Face found using full-page detection."
                            );

                            return saveFaceCrop(
                                    pageImage,
                                    candidate.rect,
                                    customerId,
                                    requestId
                            );
                        }

                        /*
                         * =================================================
                         * THIRD:
                         * Enlarged full page.
                         * =================================================
                         */
                        result =
                                tryEnlargedPage(
                                        pageImage,
                                        customerId,
                                        requestId
                                );

                        if (result != null) {

                            return result;
                        }

                    } finally {

                        pageImage.release();
                    }

                } finally {

                    Files.deleteIfExists(temp);
                }
            }
        }

        throw new IllegalArgumentException(
                "No valid face was detected in the uploaded PDF. "
                        + "The PDF was processed but the portrait "
                        + "could not be extracted."
        );
    }

    // ============================================================
    // AADHAAR PORTRAIT REGION
    // ============================================================

    private String extractAadhaarPortraitRegion(
            Mat pageImage,
            String customerId,
            Long requestId)
            throws IOException {

        int width =
                pageImage.cols();

        int height =
                pageImage.rows();

        /*
         * Based on the Aadhaar layout in the uploaded PDF:
         *
         * portrait is approximately:
         *
         * x      = 5% of page width
         * y      = 20% of page height
         * width  = 32% of page width
         * height = 55% of page height
         *
         * We intentionally make the region larger so that
         * small layout differences do not remove the portrait.
         */
        int x =
                (int) (width * 0.02);

        int y =
                (int) (height * 0.17);

        int regionWidth =
                (int) (width * 0.38);

        int regionHeight =
                (int) (height * 0.62);

        /*
         * Keep the rectangle inside the page.
         */
        x =
                Math.max(
                        0,
                        Math.min(
                                x,
                                width - 1
                        )
                );

        y =
                Math.max(
                        0,
                        Math.min(
                                y,
                                height - 1
                        )
                );

        regionWidth =
                Math.min(
                        regionWidth,
                        width - x
                );

        regionHeight =
                Math.min(
                        regionHeight,
                        height - y
                );

        if (regionWidth <= 50
                || regionHeight <= 50) {

            return null;
        }

        Rect portraitRegion =
                new Rect(
                        x,
                        y,
                        regionWidth,
                        regionHeight
                );

        System.out.println(
                "Trying Aadhaar portrait region: "
                        + portraitRegion
        );

        Mat portrait =
                new Mat(
                        pageImage,
                        portraitRegion
                ).clone();

        try {

            /*
             * Save/debug detection on the portrait region.
             */
            FaceCandidate candidate =
                    findBestFace(
                            portrait
                    );

            if (candidate != null) {

                System.out.println(
                        "FACE FOUND INSIDE AADHAAR PORTRAIT REGION"
                );

                Rect globalRect =
                        new Rect(
                                portraitRegion.x
                                        + candidate.rect.x,
                                portraitRegion.y
                                        + candidate.rect.y,
                                candidate.rect.width,
                                candidate.rect.height
                        );

                return saveFaceCrop(
                        pageImage,
                        globalRect,
                        customerId,
                        requestId
                );
            }

            /*
             * The face can be too small for YuNet.
             *
             * Enlarge the portrait region and detect again.
             */
            Mat enlarged =
                    new Mat();

            try {

                Imgproc.resize(
                        portrait,
                        enlarged,
                        new Size(
                                portrait.cols() * 4.0,
                                portrait.rows() * 4.0
                        ),
                        0,
                        0,
                        Imgproc.INTER_CUBIC
                );

                System.out.println(
                        "Trying enlarged Aadhaar portrait: "
                                + enlarged.cols()
                                + " x "
                                + enlarged.rows()
                );

                candidate =
                        findBestFace(
                                enlarged
                        );

                if (candidate != null) {

                    /*
                     * Convert enlarged coordinates back
                     * to portrait coordinates.
                     */
                    double scale = 4.0;

                    Rect local =
                            new Rect(
                                    (int)
                                            Math.round(
                                                    candidate.rect.x
                                                            / scale
                                            ),
                                    (int)
                                            Math.round(
                                                    candidate.rect.y
                                                            / scale
                                            ),
                                    (int)
                                            Math.round(
                                                    candidate.rect.width
                                                            / scale
                                            ),
                                    (int)
                                            Math.round(
                                                    candidate.rect.height
                                                            / scale
                                            )
                            );

                    local =
                            clampRect(
                                    local,
                                    portrait
                            );

                    if (local != null) {

                        Rect global =
                                new Rect(
                                        portraitRegion.x
                                                + local.x,
                                        portraitRegion.y
                                                + local.y,
                                        local.width,
                                        local.height
                                );

                        System.out.println(
                                "FACE FOUND AFTER ENLARGING "
                                        + "AADHAAR PORTRAIT"
                        );

                        return saveFaceCrop(
                                pageImage,
                                global,
                                customerId,
                                requestId
                        );
                    }
                }

            } finally {

                enlarged.release();
            }

            /*
             * IMPORTANT FALLBACK:
             *
             * Even if YuNet cannot detect the tiny face,
             * the Aadhaar portrait region itself is still a
             * valid portrait reference.
             *
             * Save the portrait region and let
             * FaceVerificationService perform its own
             * detection.
             */
            System.out.println(
                    "YuNet could not detect face inside "
                            + "Aadhaar portrait region."
            );

            System.out.println(
                    "Saving the Aadhaar portrait region "
                            + "as proof-face fallback."
            );

            return saveImage(
                    portrait,
                    customerId,
                    requestId
            );

        } finally {

            portrait.release();
        }
    }

    // ============================================================
    // NORMAL IMAGE
    // ============================================================

    private String extractFromImage(
            Path imagePath,
            String customerId,
            Long requestId)
            throws IOException {

        Mat image =
                Imgcodecs.imread(
                        imagePath.toString(),
                        Imgcodecs.IMREAD_COLOR
                );

        if (image.empty()) {

            image.release();

            throw new IOException(
                    "Unable to read uploaded proof image."
            );
        }

        try {

            System.out.println(
                    "Image size: "
                            + image.cols()
                            + " x "
                            + image.rows()
            );

            FaceCandidate candidate =
                    findBestFace(
                            image
                    );

            if (candidate != null) {

                return saveFaceCrop(
                        image,
                        candidate.rect,
                        customerId,
                        requestId
                );
            }

            /*
             * If no face is detected, enlarge and try again.
             */
            Mat enlarged =
                    new Mat();

            try {

                Imgproc.resize(
                        image,
                        enlarged,
                        new Size(
                                image.cols() * 3.0,
                                image.rows() * 3.0
                        ),
                        0,
                        0,
                        Imgproc.INTER_CUBIC
                );

                candidate =
                        findBestFace(
                                enlarged
                        );

                if (candidate != null) {

                    Rect original =
                            new Rect(
                                    (int)
                                            (candidate.rect.x / 3.0),
                                    (int)
                                            (candidate.rect.y / 3.0),
                                    (int)
                                            (candidate.rect.width / 3.0),
                                    (int)
                                            (candidate.rect.height / 3.0)
                            );

                    return saveFaceCrop(
                            image,
                            original,
                            customerId,
                            requestId
                    );
                }

            } finally {

                enlarged.release();
            }

            throw new IllegalArgumentException(
                    "No face detected in the uploaded image."
            );

        } finally {

            image.release();
        }
    }

    // ============================================================
    // ENLARGED PAGE
    // ============================================================

    private String tryEnlargedPage(
            Mat pageImage,
            String customerId,
            Long requestId)
            throws IOException {

        Mat enlarged =
                new Mat();

        try {

            Imgproc.resize(
                    pageImage,
                    enlarged,
                    new Size(
                            pageImage.cols() * 2.0,
                            pageImage.rows() * 2.0
                    ),
                    0,
                    0,
                    Imgproc.INTER_CUBIC
            );

            FaceCandidate candidate =
                    findBestFace(
                            enlarged
                    );

            if (candidate == null) {

                return null;
            }

            Rect original =
                    new Rect(
                            (int)
                                    (candidate.rect.x / 2.0),
                            (int)
                                    (candidate.rect.y / 2.0),
                            (int)
                                    (candidate.rect.width / 2.0),
                            (int)
                                    (candidate.rect.height / 2.0)
                    );

            return saveFaceCrop(
                    pageImage,
                    original,
                    customerId,
                    requestId
            );

        } finally {

            enlarged.release();
        }
    }

    // ============================================================
    // FACE DETECTION
    // ============================================================

    private FaceCandidate findBestFace(
            Mat image) {

        if (image == null
                || image.empty()) {

            return null;
        }

        if (image.cols() < 40
                || image.rows() < 40) {

            return null;
        }

        faceDetector.setInputSize(
                new Size(
                        image.cols(),
                        image.rows()
                )
        );

        Mat detections =
                new Mat();

        try {

            faceDetector.detect(
                    image,
                    detections
            );

            if (detections.empty()) {

                System.out.println(
                        "YuNet raw detections: 0"
                );

                return null;
            }

            System.out.println(
                    "YuNet raw detections: "
                            + detections.rows()
            );

            FaceCandidate best =
                    null;

            for (int i = 0;
                 i < detections.rows();
                 i++) {

                double[] data =
                        detections.get(i, 0);

                if (data == null
                        || data.length < 15) {

                    continue;
                }

                double confidence =
                        data[14];

                int x =
                        (int)
                                Math.round(
                                        data[0]
                                );

                int y =
                        (int)
                                Math.round(
                                        data[1]
                                );

                int w =
                        (int)
                                Math.round(
                                        data[2]
                                );

                int h =
                        (int)
                                Math.round(
                                        data[3]
                                );

                System.out.println(
                        "Detection "
                                + i
                                + ": x="
                                + x
                                + ", y="
                                + y
                                + ", w="
                                + w
                                + ", h="
                                + h
                                + ", confidence="
                                + confidence
                );

                if (confidence < MIN_FACE_CONFIDENCE) {
                    continue;
                }

                if (w < 15 || h < 15) {
                    continue;
                }

                Rect rect =
                        new Rect(
                                x,
                                y,
                                w,
                                h
                        );

                rect =
                        clampRect(
                                rect,
                                image
                        );

                if (rect == null) {
                    continue;
                }

                /*
                 * Score:
                 * confidence + face size.
                 */
                double area =
                        (double)
                                rect.width
                                * rect.height;

                double imageArea =
                        (double)
                                image.cols()
                                * image.rows();

                double areaRatio =
                        area / imageArea;

                double score =
                        confidence * 0.80
                                +
                                Math.min(
                                        areaRatio * 10.0,
                                        1.0
                                ) * 0.20;

                FaceCandidate candidate =
                        new FaceCandidate(
                                rect,
                                confidence,
                                score
                        );

                if (best == null
                        || candidate.score
                        > best.score) {

                    best = candidate;
                }
            }

            return best;

        } finally {

            detections.release();
        }
    }

    // ============================================================
    // SAVE FACE CROP
    // ============================================================

    private String saveFaceCrop(
            Mat image,
            Rect face,
            String customerId,
            Long requestId)
            throws IOException {

        face =
                clampRect(
                        face,
                        image
                );

        if (face == null) {

            throw new IOException(
                    "Invalid detected face rectangle."
            );
        }

        /*
         * Add margin around face.
         */
        int marginX =
                (int)
                        (face.width * 0.45);

        int marginY =
                (int)
                        (face.height * 0.55);

        int x =
                Math.max(
                        0,
                        face.x - marginX
                );

        int y =
                Math.max(
                        0,
                        face.y - marginY
                );

        int right =
                Math.min(
                        image.cols(),
                        face.x
                                + face.width
                                + marginX
                );

        int bottom =
                Math.min(
                        image.rows(),
                        face.y
                                + face.height
                                + marginY
                );

        Rect crop =
                new Rect(
                        x,
                        y,
                        right - x,
                        bottom - y
                );

        Mat faceImage =
                new Mat(
                        image,
                        crop
                ).clone();

        try {

            return saveImage(
                    faceImage,
                    customerId,
                    requestId
            );

        } finally {

            faceImage.release();
        }
    }

    // ============================================================
    // SAVE IMAGE
    // ============================================================

    private String saveImage(
            Mat image,
            String customerId,
            Long requestId)
            throws IOException {

        if (image == null
                || image.empty()) {

            throw new IOException(
                    "Proof-face image is empty."
            );
        }

        Mat output =
                new Mat();

        try {

            double scale =
                    Math.min(
                            (double)
                                    MAX_OUTPUT_SIZE
                                    / image.cols(),
                            (double)
                                    MAX_OUTPUT_SIZE
                                    / image.rows()
                    );

            if (scale > 1.0) {
                scale = 1.0;
            }

            int width =
                    Math.max(
                            160,
                            (int)
                                    Math.round(
                                            image.cols()
                                                    * scale
                                    )
                    );

            int height =
                    Math.max(
                            160,
                            (int)
                                    Math.round(
                                            image.rows()
                                                    * scale
                                    )
                    );

            Imgproc.resize(
                    image,
                    output,
                    new Size(
                            width,
                            height
                    ),
                    0,
                    0,
                    Imgproc.INTER_CUBIC
            );

            String safeCustomerId =
                    sanitize(
                            customerId
                    );

            String request =
                    requestId == null
                            ? "unknown"
                            : requestId.toString();

            /*
             * IMPORTANT:
             *
             * Every request gets a completely new file.
             */
            String fileName =
                    safeCustomerId
                            + "_"
                            + request
                            + "_proof_face_"
                            + System.currentTimeMillis()
                            + ".jpg";

            Path outputPath =
                    Paths.get(
                            OUTPUT_DIRECTORY,
                            fileName
                    )
                    .toAbsolutePath()
                    .normalize();

            boolean written =
                    Imgcodecs.imwrite(
                            outputPath.toString(),
                            output
                    );

            if (!written) {

                throw new IOException(
                        "OpenCV failed to write: "
                                + outputPath
                );
            }

            if (!Files.exists(outputPath)) {

                throw new IOException(
                        "Proof-face file was not created."
                );
            }

            long fileSize =
                    Files.size(
                            outputPath
                    );

            if (fileSize <= 0) {

                throw new IOException(
                        "Proof-face file is empty."
                );
            }

            System.out.println();
            System.out.println(
                    "================================================"
            );
            System.out.println(
                    "NEW PROOF FACE CREATED"
            );
            System.out.println(
                    "Customer : " + customerId
            );
            System.out.println(
                    "Request  : " + requestId
            );
            System.out.println(
                    "File     : " + outputPath
            );
            System.out.println(
                    "Size     : " + fileSize + " bytes"
            );
            System.out.println(
                    "Image    : "
                            + output.cols()
                            + " x "
                            + output.rows()
            );
            System.out.println(
                    "================================================"
            );
            System.out.println();

            return outputPath.toString();

        } finally {

            output.release();
        }
    }

    // ============================================================
    // RECTANGLE CLAMP
    // ============================================================

    private Rect clampRect(
            Rect rect,
            Mat image) {

        if (rect == null
                || image == null
                || image.empty()) {

            return null;
        }

        int x =
                Math.max(
                        0,
                        Math.min(
                                rect.x,
                                image.cols() - 1
                        )
                );

        int y =
                Math.max(
                        0,
                        Math.min(
                                rect.y,
                                image.rows() - 1
                        )
                );

        int width =
                Math.min(
                        rect.width,
                        image.cols() - x
                );

        int height =
                Math.min(
                        rect.height,
                        image.rows() - y
                );

        if (width <= 0
                || height <= 0) {

            return null;
        }

        return new Rect(
                x,
                y,
                width,
                height
        );
    }

    // ============================================================
    // EXTENSION
    // ============================================================

    private String getExtension(
            String fileName) {

        if (fileName == null) {
            return "";
        }

        int index =
                fileName.lastIndexOf('.');

        if (index < 0) {
            return "";
        }

        return fileName
                .substring(index + 1)
                .toLowerCase();
    }

    // ============================================================
    // SANITIZE
    // ============================================================

    private String sanitize(
            String value) {

        if (value == null
                || value.isBlank()) {

            return "customer";
        }

        return value.replaceAll(
                "[^a-zA-Z0-9_-]",
                "_"
        );
    }

    // ============================================================
    // FACE CANDIDATE
    // ============================================================

    private static class FaceCandidate {

        private final Rect rect;

        private final double confidence;

        private final double score;

        private FaceCandidate(
                Rect rect,
                double confidence,
                double score) {

            this.rect = rect;
            this.confidence = confidence;
            this.score = score;
        }
    }
}