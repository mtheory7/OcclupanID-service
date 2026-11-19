package com.mtheory7.service;

import com.mtheory7.entites.PredictionResponse;
import com.mtheory7.entites.PredictionResponse.CandidatePrediction;
import jakarta.annotation.PostConstruct;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OcclupanIDService {
    private static final int IMAGE_HEIGHT = 224;
    private static final int IMAGE_WIDTH = 224;
    private static final int CHANNELS = 3;

    @Value("${occlupanid.model-path}")
    private Resource modelResource;

    @Value("${occlupanid.class-names}")
    private List<String> classNames;

    private ComputationGraph model;
    private final ReentrantLock inferenceLock = new ReentrantLock(true);

    @PostConstruct
    public void init() throws IOException {
        try (InputStream is = modelResource.getInputStream()) {
            // loadUpdater = false skips Adam optimizer state, cutting inference RAM in half
            this.model = ModelSerializer.restoreComputationGraph(is, false);
        }
    }

    public PredictionResponse classify(InputStream imageStream) throws IOException {
        long startNs = System.nanoTime();

        NativeImageLoader loader = new NativeImageLoader(IMAGE_HEIGHT, IMAGE_WIDTH, CHANNELS);
        INDArray features = loader.asMatrix(imageStream); // [1, 3, 224, 224]
        INDArray probabilities = null;

        try {
            // Must match VGG16ImagePreProcessor used during training
            VGG16ImagePreProcessor scaler = new VGG16ImagePreProcessor();
            scaler.preProcess(features);

            // ComputationGraph is not thread-safe; serialize forward passes safely
            inferenceLock.lock();
            try {
                probabilities = model.outputSingle(features);
            } finally {
                inferenceLock.unlock();
            }

            List<CandidatePrediction> ranked = new ArrayList<>(classNames.size());
            for (int i = 0; i < classNames.size(); i++) {
                ranked.add(new CandidatePrediction(classNames.get(i), probabilities.getDouble(0, i)));
            }
            ranked.sort(Comparator.comparingDouble(CandidatePrediction::probability).reversed());

            CandidatePrediction best = ranked.get(0);
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

            return new PredictionResponse(
                    best.className(),
                    best.probability(),
                    elapsedMs,
                    ranked.subList(0, Math.min(3, ranked.size()))
            );
        } finally {
            if (features != null && !features.wasClosed()) {
                features.close();
            }
            if (probabilities != null && !probabilities.wasClosed()) {
                probabilities.close();
            }
        }
    }
}