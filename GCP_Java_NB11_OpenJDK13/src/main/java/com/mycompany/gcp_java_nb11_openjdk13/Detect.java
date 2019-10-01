package com.mycompany.gcp_java_nb11_openjdk13;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateFileResponse.Builder;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.AsyncAnnotateFileResponse;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.CropHint;
import com.google.cloud.vision.v1.CropHintsAnnotation;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.LocationInfo;
import com.google.cloud.vision.v1.OperationMetadata;
import com.google.cloud.vision.v1.OutputConfig;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebEntity;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebLabel;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.cloud.vision.v1.WebDetectionParams;
import com.google.cloud.vision.v1.Word;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Detect {
    public static void main(String[] args) throws Exception, IOException {
        argsHelper(args, System.out);
    }

    public static void argsHelper(String[] args, PrintStream out) throws Exception, IOException {
        if (args.length < 1) {
            out.println("Usage:");
            out.printf(
                "\tmvn exec:java -DDetect -Dexec.args=\"<command> <path-to-image>\"\n"
                + "\tmvn exec:java -DDetect -Dexec.args=\"ocr <path-to-file> <path-to-destination>\""
                + "\n"
                + "Commands:\n"
                + "\tfaces | labels | landmarks | logos | text | safe-search | properties"
                + "| web | web-entities | web-entities-include-geo | crop | ocr \n"
                + "| object-localization \n"
                + "Path:\n\tA file path (ex: ./resources/wakeupcat.jpg) or a URI for a Cloud Storage "
                + "resource (gs://...)\n"
                + "Path to File:\n\tA path to the remote file on Cloud Storage (gs://...)\n"
                + "Path to Destination\n\tA path to the remote destination on Cloud Storage for the"
                + " file to be saved. (gs://BUCKET_NAME/PREFIX/)\n");
            return;
        }
        
        String command = args[0];
        String path = args.length > 1 ? args[1] : "";

        if (command.equals("faces")) {
            if (path.startsWith("gs://"))
                detectFacesGcs(path, out);
            else
                detectFaces(path, out);
        }
        else if (command.equals("labels")) {
            if (path.startsWith("gs://"))
                detectLabelsGcs(path, out);
            else
                detectLabels(path, out);
        }
        else if (command.equals("landmarks")) {
            if (path.startsWith("http"))
                detectLandmarksUrl(path, out);
            else if (path.startsWith("gs://"))
                detectLandmarksGcs(path, out);
            else
                detectLandmarks(path, out);
        }
        else if (command.equals("logos")) {
            if (path.startsWith("gs://"))
                detectLogosGcs(path, out);
            else
                detectLogos(path, out);
        }
        else if (command.equals("text")) {
            if (path.startsWith("gs://"))
                detectTextGcs(path, out);
            else
                detectText(path, out);
        }
        else if (command.equals("properties")) {
            if (path.startsWith("gs://"))
                detectPropertiesGcs(path, out);
            else
                detectProperties(path, out);
        }
        else if (command.equals("safe-search")) {
            if (path.startsWith("gs://"))
                detectSafeSearchGcs(path, out);
            else
                detectSafeSearch(path, out);
        }
        else if (command.equals("web")) {
            if (path.startsWith("gs://"))
                detectWebDetectionsGcs(path, out);
            else
                detectWebDetections(path, out);
        }
        else if (command.equals("web-entities")) {
            if (path.startsWith("gs://"))
                detectWebEntitiesGcs(path, out);
            else
                detectWebEntities(path, out);
        }
        else if (command.equals("web-entities-include-geo")) {
            if (path.startsWith("gs://"))
                detectWebEntitiesIncludeGeoResultsGcs(path, out);
            else
                detectWebEntitiesIncludeGeoResults(path, out);
        }
        else if (command.equals("crop")) {
            if (path.startsWith("gs://"))
                detectCropHintsGcs(path, out);
            else
                detectCropHints(path, out);
        }
        else if (command.equals("fulltext")) {
            if (path.startsWith("gs://"))
                detectDocumentTextGcs(path, out);
            else
                detectDocumentText(path, out);
        }
        else if (command.equals("ocr")) {
            String destPath = args.length > 2 ? args[2] : "";
            detectDocumentsGcs(path, destPath);
        }
        else if (command.equals("object-localization")) {
            if (path.startsWith("gs://"))
                detectLocalizedObjectsGcs(path, out);
            else
                detectLocalizedObjects(path, out);
        }
    }

    public static void detectFaces(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.FACE_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
                    out.printf(
                    "anger: %s\njoy: %s\nsurprise: %s\nposition: %s",
                    annotation.getAngerLikelihood(),
                    annotation.getJoyLikelihood(),
                    annotation.getSurpriseLikelihood(),
                    annotation.getBoundingPoly());
                }
            }
        }
    }

    public static void detectFacesGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.FACE_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
                    out.printf(
                        "anger: %s\njoy: %s\nsurprise: %s\nposition: %s",
                        annotation.getAngerLikelihood(),
                        annotation.getJoyLikelihood(),
                        annotation.getSurpriseLikelihood(),
                        annotation.getBoundingPoly());
                }
            }
        }
    }

    public static void detectLabels(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLabelAnnotationsList())
                    annotation.getAllFields().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
            }
        }
    }

    public static void detectLabelsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLabelAnnotationsList())
                    annotation.getAllFields().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
            }
        }
    }

    public static void detectLandmarks(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = annotation.getLocationsList().listIterator().next();
                    out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
                }
            }
        }
    }

    public static void detectLandmarksUrl(String uri, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setImageUri(uri).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = annotation.getLocationsList().listIterator().next();
                    out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
                }
            }
        }
    }

    public static void detectLandmarksGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = annotation.getLocationsList().listIterator().next();
                    out.printf("Landmark: %s\n %s\n", annotation.getDescription(), info.getLatLng());
                }
            }
        }
    }

    public static void detectLogos(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLogoAnnotationsList())
                    out.println(annotation.getDescription());
            }
        }
    }

    public static void detectLogosGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.LOGO_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getLogoAnnotationsList())
                    out.println(annotation.getDescription());
            }
        }
    }

    public static void detectText(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    out.printf("Text: %s\n", annotation.getDescription());
                    out.printf("Position : %s\n", annotation.getBoundingPoly());
                }
            }
        }
    }

    public static void detectTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    out.printf("Text: %s\n", annotation.getDescription());
                    out.printf("Position : %s\n", annotation.getBoundingPoly());
                }
            }
        }
    }

    public static void detectProperties(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.IMAGE_PROPERTIES).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
                for (ColorInfo color : colors.getColorsList()) {
                    out.printf(
                    "fraction: %f\nr: %f, g: %f, b: %f\n",
                    color.getPixelFraction(),
                    color.getColor().getRed(),
                    color.getColor().getGreen(),
                    color.getColor().getBlue());
                }
            }
        }
    }

    public static void detectPropertiesGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.IMAGE_PROPERTIES).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
                for (ColorInfo color : colors.getColorsList()) {
                    out.printf(
                        "fraction: %f\nr: %f, g: %f, b: %f\n",
                        color.getPixelFraction(),
                        color.getColor().getRed(),
                        color.getColor().getGreen(),
                        color.getColor().getBlue());
                }
            }
        }
    }

    public static void detectSafeSearch(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
                out.printf(
                    "adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\nracy: %s\n",
                    annotation.getAdult(),
                    annotation.getMedical(),
                    annotation.getSpoof(),
                    annotation.getViolence(),
                    annotation.getRacy());
            }
        }
    }

    public static void detectSafeSearchGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.SAFE_SEARCH_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
                out.printf(
                    "adult: %s\nmedical: %s\nspoofed: %s\nviolence: %s\nracy: %s\n",
                    annotation.getAdult(),
                    annotation.getMedical(),
                    annotation.getSpoof(),
                    annotation.getViolence(),
                    annotation.getRacy());
            }
        }
    }

    public static void detectWebDetections(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.WEB_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                WebDetection annotation = res.getWebDetection();
                out.println("Entity:Id:Score");
                out.println("===============");
                for (WebEntity entity : annotation.getWebEntitiesList())
                    out.println(entity.getDescription() + " : " + entity.getEntityId() + " : " + entity.getScore());
                for (WebLabel label : annotation.getBestGuessLabelsList())
                    out.format("\nBest guess label: %s", label.getLabel());
                out.println("\nPages with matching images: Score\n==");
                for (WebPage page : annotation.getPagesWithMatchingImagesList())
                    out.println(page.getUrl() + " : " + page.getScore());
                out.println("\nPages with partially matching images: Score\n==");
                for (WebImage image : annotation.getPartialMatchingImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
                out.println("\nPages with fully matching images: Score\n==");
                for (WebImage image : annotation.getFullMatchingImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
                out.println("\nPages with visually similar images: Score\n==");
                for (WebImage image : annotation.getVisuallySimilarImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
            }
        }
    }

    public static void detectWebDetectionsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.WEB_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                WebDetection annotation = res.getWebDetection();
                out.println("Entity:Id:Score");
                out.println("===============");
                for (WebEntity entity : annotation.getWebEntitiesList())
                    out.println(entity.getDescription() + " : " + entity.getEntityId() + " : " + entity.getScore());
                for (WebLabel label : annotation.getBestGuessLabelsList())
                    out.format("\nBest guess label: %s", label.getLabel());
                out.println("\nPages with matching images: Score\n==");
                for (WebPage page : annotation.getPagesWithMatchingImagesList())
                    out.println(page.getUrl() + " : " + page.getScore());
                out.println("\nPages with partially matching images: Score\n==");
                for (WebImage image : annotation.getPartialMatchingImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
                out.println("\nPages with fully matching images: Score\n==");
                for (WebImage image : annotation.getFullMatchingImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
                out.println("\nPages with visually similar images: Score\n==");
                for (WebImage image : annotation.getVisuallySimilarImagesList())
                    out.println(image.getUrl() + " : " + image.getScore());
            }
        }
    }

    public static void detectWebEntities(String filePath, PrintStream out) throws Exception, IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ByteString contents = ByteString.readFrom(new FileInputStream(filePath));
            Image image = Image.newBuilder().setContent(contents).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION))
                .setImage(image).build();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Arrays.asList(request));
            response.getResponsesList().stream()
                .forEach(r -> r.getWebDetection().getWebEntitiesList().stream()
                    .forEach(entity -> {
                        out.format("Description: %s\n", entity.getDescription());
                out.format("Score: %f\n", entity.getScore());
            }));
        }
    }

    public static void detectWebEntitiesGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ImageSource imageSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
            Image image = Image.newBuilder().setSource(imageSource).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION))
                .setImage(image).build();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Arrays.asList(request));            
            response.getResponsesList().stream()
                .forEach(r -> r.getWebDetection().getWebEntitiesList().stream()
                    .forEach(entity -> {
                        System.out.format("Description: %s\n", entity.getDescription());
                    System.out.format("Score: %f\n", entity.getScore());
            }));
        }
    }

    public static void detectWebEntitiesIncludeGeoResults(String filePath, PrintStream out) throws Exception, IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ByteString contents = ByteString.readFrom(new FileInputStream(filePath));
            Image image = Image.newBuilder().setContent(contents).build();
            WebDetectionParams webDetectionParams = WebDetectionParams.newBuilder()
                .setIncludeGeoResults(true).build();
            ImageContext imageContext = ImageContext.newBuilder()
                .setWebDetectionParams(webDetectionParams).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION))
                .setImage(image).setImageContext(imageContext).build();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Arrays.asList(request));
            response.getResponsesList().stream()
                .forEach(r -> r.getWebDetection().getWebEntitiesList().stream()
                    .forEach(entity -> {
                        out.format("Description: %s\n", entity.getDescription());
                out.format("Score: %f\n", entity.getScore());
            }));
        }
    }

    public static void detectWebEntitiesIncludeGeoResultsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ImageSource imageSource = ImageSource.newBuilder()
                .setGcsImageUri(gcsPath).build();
            Image image = Image.newBuilder().setSource(imageSource).build();
            WebDetectionParams webDetectionParams = WebDetectionParams.newBuilder()
                .setIncludeGeoResults(true).build();
            ImageContext imageContext = ImageContext.newBuilder()
                .setWebDetectionParams(webDetectionParams).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(Feature.newBuilder().setType(Type.WEB_DETECTION))
                .setImage(image).setImageContext(imageContext).build();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(Arrays.asList(request));
            response.getResponsesList().stream()
                .forEach(r -> r.getWebDetection().getWebEntitiesList().stream()
                    .forEach(entity -> {
                        out.format("Description: %s\n", entity.getDescription());
                    out.format("Score: %f\n", entity.getScore());
            }));
        }
    }

    public static void detectCropHints(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.CROP_HINTS).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                CropHintsAnnotation annotation = res.getCropHintsAnnotation();
                for (CropHint hint : annotation.getCropHintsList())
                    out.println(hint.getBoundingPoly());
            }
        }
    }

    public static void detectCropHintsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.CROP_HINTS).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                CropHintsAnnotation annotation = res.getCropHintsAnnotation();
                for (CropHint hint : annotation.getCropHintsList()) {
                    out.println(hint.getBoundingPoly());
                }
            }
        }
    }

    public static void detectDocumentText(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            client.close();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page: annotation.getPagesList()) {
                    String pageText = "";
                    for (Block block : page.getBlocksList()) {
                        String blockText = "";
                        for (Paragraph para : block.getParagraphsList()) {
                            String paraText = "";
                            for (Word word: para.getWordsList()) {
                                String wordText = "";
                                for (Symbol symbol: word.getSymbolsList()) {
                                    wordText = wordText + symbol.getText();
                                    out.format("Symbol text: %s (confidence: %f)\n", symbol.getText(), symbol.getConfidence());
                                }
                                out.format("Word text: %s (confidence: %f)\n\n", wordText, word.getConfidence());
                                paraText = String.format("%s %s", paraText, wordText);
                            }
                            out.println("\nParagraph: \n" + paraText);
                            out.format("Paragraph Confidence: %f\n", para.getConfidence());
                            blockText = blockText + paraText;
                        }
                        pageText = pageText + blockText;
                    }
                }
                out.println("\nComplete annotation:");
                out.println(annotation.getText());
            }
        }
    }

    public static void detectDocumentTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            client.close();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page: annotation.getPagesList()) {
                    String pageText = "";
                    for (Block block : page.getBlocksList()) {
                        String blockText = "";
                        for (Paragraph para : block.getParagraphsList()) {
                            String paraText = "";
                            for (Word word: para.getWordsList()) {
                                String wordText = "";
                                for (Symbol symbol: word.getSymbolsList()) {
                                    wordText = wordText + symbol.getText();
                                    out.format("Symbol text: %s (confidence: %f)\n", symbol.getText(), symbol.getConfidence());
                                }
                                out.format("Word text: %s (confidence: %f)\n\n", wordText, word.getConfidence());
                                paraText = String.format("%s %s", paraText, wordText);
                            }
                            out.println("\nParagraph: \n" + paraText);
                            out.format("Paragraph Confidence: %f\n", para.getConfidence());
                            blockText = blockText + paraText;
                        }
                        pageText = pageText + blockText;
                    }
                }
                out.println("\nComplete annotation:");
                out.println(annotation.getText());
            }
        }
    }

    public static void detectDocumentsGcs(String gcsSourcePath, String gcsDestinationPath) throws Exception {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            List<AsyncAnnotateFileRequest> requests = new ArrayList<>();
            GcsSource gcsSource = GcsSource.newBuilder()
                .setUri(gcsSourcePath).build();
            InputConfig inputConfig = InputConfig.newBuilder()
                .setMimeType("application/pdf")
                .setGcsSource(gcsSource).build();
            GcsDestination gcsDestination = GcsDestination.newBuilder()
                .setUri(gcsDestinationPath).build();
            OutputConfig outputConfig = OutputConfig.newBuilder()
                .setBatchSize(2).setGcsDestination(gcsDestination).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AsyncAnnotateFileRequest request = AsyncAnnotateFileRequest.newBuilder()
                .addFeatures(feature).setInputConfig(inputConfig)
                .setOutputConfig(outputConfig).build();
            requests.add(request);
            OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> response = client.asyncBatchAnnotateFilesAsync(requests);
            System.out.println("Waiting for the operation to finish.");
            List<AsyncAnnotateFileResponse> result = response.get(180, TimeUnit.SECONDS).getResponsesList();
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Pattern pattern = Pattern.compile("gs://([^/]+)/(.+)");
            Matcher matcher = pattern.matcher(gcsDestinationPath);
            if (matcher.find()) {
                String bucketName = matcher.group(1);
                String prefix = matcher.group(2);
                Bucket bucket = storage.get(bucketName);
                com.google.api.gax.paging.Page<Blob> pageList = bucket.list(BlobListOption.prefix(prefix));
                Blob firstOutputFile = null;
                System.out.println("Output files:");
                for (Blob blob : pageList.iterateAll()) {
                    System.out.println(blob.getName());
                    if (firstOutputFile == null)
                        firstOutputFile = blob;
                }
                String jsonContents = new String(firstOutputFile.getContent());
                Builder builder = AnnotateFileResponse.newBuilder();
                JsonFormat.parser().merge(jsonContents, builder);
                AnnotateFileResponse annotateFileResponse = builder.build();
                AnnotateImageResponse annotateImageResponse = annotateFileResponse.getResponses(0);
                System.out.format("\nText: %s\n", annotateImageResponse.getFullTextAnnotation().getText());
            }
            else
                System.out.println("No MATCH");
        }
    }

    public static void detectLocalizedObjects(String filePath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION))
            .setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                for (LocalizedObjectAnnotation entity : res.getLocalizedObjectAnnotationsList()) {
                    out.format("Object name: %s\n", entity.getName());
                    out.format("Confidence: %s\n", entity.getScore());
                    out.format("Normalized Vertices:\n");
                    entity.getBoundingPoly().getNormalizedVerticesList()
                        .forEach(vertex -> out.format("- (%s, %s)\n", vertex.getX(), vertex.getY()));
                }
            }
        }
    }

    public static void detectLocalizedObjectsGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION))
            .setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            client.close();
            for (AnnotateImageResponse res : responses) {
                for (LocalizedObjectAnnotation entity : res.getLocalizedObjectAnnotationsList()) {
                    out.format("Object name: %s\n", entity.getName());
                    out.format("Confidence: %s\n", entity.getScore());
                    out.format("Normalized Vertices:\n");
                    entity.getBoundingPoly().getNormalizedVerticesList()                        
                        .forEach(vertex -> out.format("- (%s, %s)\n", vertex.getX(), vertex.getY()));
                }
            }
        }
    }
}
