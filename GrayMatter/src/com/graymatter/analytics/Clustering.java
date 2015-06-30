package com.graymatter.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;


public class Clustering {
	
	private Preprocessing preprocessing = new Preprocessing();

	public enum SimilarityMetric {
		COSINE, EUCLIDIAN
	};

	// Number of clusters.
	private int K;
	private SimilarityMetric similarityMetric;
	private int iterations;

	private List<String> thesaurus = null;
	private List<HashMap<String, Double>> centroids = null;
	private Map<String, HashMap<String, Integer>> featureVectors = null;
	private Map<String, Integer> clusters = null;

	public Clustering(int iterations, SimilarityMetric similarityMetric) {
		this.similarityMetric = similarityMetric;
		this.iterations=iterations;
	}

	/**
	 * This function takes a List of documents preprocesses each document by
	 * removing stop words and then builds a thesaurus of words contained in all
	 * the documents. This thesaurus acts a vocabulary for clustering.
	 * 
	 * @param documents
	 * @return
	 */
	private List<String> createThesaurusTSV(List<String> documents) {
		Set<String> thesaurus = new HashSet<String>();
		for (String doc : documents) {
			String[] words = doc.trim().toLowerCase().split(" ");
			thesaurus.addAll(Arrays.asList(words));
		}
		return new ArrayList<String>(thesaurus);
	}

	/**
	 * This function takes a list of documents. Finds count of each word in a
	 * document and creates a sparse array of word count. This array is stored
	 * in the form of a {@code Map<String, HashMap<String, Integer>>} with
	 * specification <Summary,<word,count>>
	 * 
	 * @param documents
	 * @return
	 */
	private Map<String, HashMap<String, Integer>> convertDocumentToFeatureVector(
			List<String> documents) {

		Map<String, HashMap<String, Integer>> featureVectors = new HashMap<String, HashMap<String, Integer>>();
		for (String doc : documents) {
			String[] words = doc.trim().toLowerCase().split(" ");
			HashMap<String, Integer> sparseVector = new HashMap<String, Integer>();
			for (String word : words) {
				if (sparseVector.containsKey(word))
					sparseVector.put(word, sparseVector.get(word) + 1);
				else
					sparseVector.put(word, 1);
			}
			featureVectors.put(doc, sparseVector);
		}
		return featureVectors;
	}

	/**
	 * This function takes {@code featureVectors} that is a sparse vector
	 * contaning count of each word in the document. Takes {@code K} such
	 * vectors and creates them as initial centroids.
	 * 
	 * @param featureVectors
	 */
	private List<HashMap<String, Double>> initializeCentroids(
			Map<String, HashMap<String, Integer>> featureVectors) {
		List<HashMap<String, Double>> centroids = new ArrayList<HashMap<String, Double>>();

		// Initialize a list of K centroids with zero initial values for
		// attributes.
		for (int i = 0; i < this.K; i++) {
			HashMap<String, Double> attributeVals = new HashMap<String, Double>();
			for (String word : this.thesaurus)
				attributeVals.put(word, 0.0);
			// Add ith centroid to centroid list.
			centroids.add(attributeVals);
		}

		// Take K random samples.
		List<String> sampleKeys = new ArrayList<String>(featureVectors.keySet());
		List<HashMap<String, Integer>> centroidSamples = new ArrayList<HashMap<String, Integer>>();
		Random rand = new Random();
		for (int i = 0; i < this.K; i++) {
			int randomInt = rand.nextInt(featureVectors.size()) + 0;
			centroidSamples.add(featureVectors.get(sampleKeys.get(randomInt)));
		}

		// Add values of K samples selected above to centroids.
		for (int i = 0; i < this.K; i++) {
			Map<String, Double> centroidAttribVals = centroids.get(i);
			HashMap<String, Integer> sampleVals = centroidSamples.get(i);
			for (Map.Entry<String, Integer> entrySet : sampleVals.entrySet()) {
				centroidAttribVals.put(
						entrySet.getKey(),
						centroidAttribVals.get(entrySet.getKey())
								+ entrySet.getValue());
			}
		}
		System.out.println(centroidSamples);
		System.out.println(centroids);
		return centroids;
	}

	/**
	 * This method computes COSINE SIMILARITY between {@code sample} and
	 * {@code centroid}
	 * 
	 * @param sample
	 * @param centroid
	 * @return
	 */
	private double computeCosineSimilarity(Map<String, Integer> sample,
			Map<String, Double> centroid) {
		// length of vector.
		double modVector = 0.0;
		for (Map.Entry<String, Integer> entrySet : sample.entrySet())
			modVector += (entrySet.getValue() * entrySet.getValue());
		modVector = Math.sqrt(modVector);
		// length of curCentroid.
		double modCurCentroid = 0.0;
		for (Map.Entry<String, Double> entrySet : centroid.entrySet())
			modCurCentroid += entrySet.getValue() * entrySet.getValue();
		modCurCentroid = Math.sqrt(modCurCentroid);
		// Compute similarity of vector with curCentroid
		double dotProduct = 0.0;
		for (Map.Entry<String, Integer> entrySet : sample.entrySet())
			dotProduct += entrySet.getValue() * centroid.get(entrySet.getKey());
		// Cosine similarity.
		double COSINE_SIMILARITY = dotProduct / (modVector * modCurCentroid);

		return COSINE_SIMILARITY;
	}

	/**
	 * This method computes EUCLIDIAN DISTANCE between {@code sample} and
	 * {@code centroid}
	 * 
	 * @param sample
	 * @param centroid
	 * @return
	 */
	private double computeEuclidianSimilarity(Map<String, Integer> sample,
			Map<String, Double> centroid) {
		double coordinateDiff = 0;
		for (Map.Entry<String, Integer> entrySet : sample.entrySet()) {
			coordinateDiff += Math.pow(
					(entrySet.getValue() - centroid.get(entrySet.getKey())), 2);
		}
		double EUCLIDIAN_SIMILARITY = Math.sqrt(coordinateDiff);
		return EUCLIDIAN_SIMILARITY;
	}

	/**
	 * This function performs K-Means clustering of sample vectors stored in
	 * {@code featureVectors} and {@code centroids}. It computes centroids in
	 * each step and then centroids are updated by taking average value of
	 * attributes of all the samples that belong to a particular cluster.
	 * 
	 * @param centroids
	 * @param featureVectors
	 * @param iterations
	 * @return
	 */
	private Map<String, Integer> kMeansClustering(
			List<HashMap<String, Double>> centroids,
			Map<String, HashMap<String, Integer>> featureVectors, int iterations) {

		int iter=iterations;
		boolean iterationFlag=false;
		// Iterate and improve the clusters for 'iterations' count.
		do {
			iterationFlag=false;
			// Compute similarity of each featureVectors with the centroids.
			for (Map.Entry<String, HashMap<String, Integer>> vectorEntrySet : featureVectors
					.entrySet()) {
				String doc = vectorEntrySet.getKey();
				HashMap<String, Integer> vector = vectorEntrySet.getValue();
				double similarity = -2.0;
				for (int j = 0; j < this.centroids.size(); j++) {
					HashMap<String, Double> curCentroid = this.centroids.get(j);
					// double
					// NEW_SIMILARITY=this.computeCosineSimilarity(vector,
					// curCentroid);
					double NEW_SIMILARITY = 0;
					if (this.similarityMetric == SimilarityMetric.EUCLIDIAN)
						NEW_SIMILARITY = this.computeEuclidianSimilarity(
								vector, curCentroid);
					else if (this.similarityMetric == SimilarityMetric.COSINE)
						NEW_SIMILARITY = this.computeCosineSimilarity(vector,
								curCentroid);
					// If current centroid is more similar then past ones then
					// make current centroid count as cluster of current doc.
					if (NEW_SIMILARITY > similarity) {
						System.out.println("vector="+vector+"     doc="+doc+"     j="+j+"NEW_SIMILARITY="+NEW_SIMILARITY+"      similarity="+similarity);
						this.clusters.put(doc, j);
						similarity = NEW_SIMILARITY;
						iterationFlag=true;
						
					}
				}
			}

			// After each iteration update weights of centroids based on the
			// samples in each centroid.
			for (int j = 0; j < this.centroids.size(); j++) {
				HashMap<String, Double> curCentroid = this.centroids.get(j);
				// Re-initialize curCentroid values to zero.
				for (Map.Entry<String, Double> entrySet : curCentroid
						.entrySet())
					entrySet.setValue(0.0);

				// Now find average value of new centroid.
				int curCentroidCardinality = 0;
				// Find all samples whose cluster value is j i.e. which belong
				// to curCentroid
				for (Map.Entry<String, Integer> clusterEntrySet : this.clusters
						.entrySet()) {
					if (clusterEntrySet.getValue() == j) {
						curCentroidCardinality++;
						HashMap<String, Integer> sample = this.featureVectors
								.get(clusterEntrySet.getKey());
						// Add values of sample to new centroid.
						for (Map.Entry<String, Integer> sampleEntrySet : sample
								.entrySet()) {
							double newValue = curCentroid.get(sampleEntrySet
									.getKey()) + sampleEntrySet.getValue();
							curCentroid.put(sampleEntrySet.getKey(), newValue);
						}
					}
				}

				// If no sample belongs to this cluster then divide by zero
				// could occur and also there will not be any change in the
				// centroid coordinates thus leave it as it is.
				 if (curCentroidCardinality < 1) continue;
				 

				// curCentroid now has new sum from all samples of cluster.
				// Compute average values of each attribute to form new
				// centroid.
				for (Map.Entry<String, Double> entrySet : curCentroid
						.entrySet()) {
					double val = entrySet.getValue() / curCentroidCardinality;
					entrySet.setValue(val);
				}
			}
		}while((iter>0?--iter:0) >0 || (iterations>0?false:iterationFlag));
		return this.clusters;
	}

	
	/**
	 * This function takes a {@code List<String>} of {@code documents} and
	 * iteration count {@code iterations}. It then pre-processes raw documents,
	 * creates clusters of processed documents and returns a
	 * {@code Map<String,String>} with document as key and cluster number as
	 * value.
	 * 
	 * @param documents
	 * @param iterations
	 * @return
	 */
	private Map<String,Integer> cluster(List<String> documents, int iterations) {
		this.thesaurus = this.createThesaurusTSV(documents);
		this.featureVectors = this.convertDocumentToFeatureVector(documents);
		this.centroids = this.initializeCentroids(featureVectors);

		// Now find similarity of each sample with these centroids and then
		// after clusters are formed update centroids.
		this.clusters = new HashMap<String, Integer>();
		for (String doc : documents)
			this.clusters.put(doc, -1);
		this.clusters = this.kMeansClustering(this.centroids,
				this.featureVectors, iterations);
		System.out.println("clusters=" + this.clusters);
		System.out.println("centroids=" + this.centroids);
		
		//If single cluster is formed then re-compute kmeans.
		if(actualDistinctClusters(this.clusters)<this.K){
			cluster(documents,iterations);
		}
		
		return this.clusters;
	}
	
	public Map<String, String> runClustering(List<String> rawDocuments) {
		
		this.K = (int) Math.sqrt(rawDocuments.size() / 2);
		
		// Keys are summaries and values are pre-processed summaries.
		Map<String, String> preprocessedSummaries = this
				.createClusteringInput(rawDocuments);

		// Now cluster value list of above Map.
		Map<String, Integer> clusters = this.cluster(
				new ArrayList<String>(preprocessedSummaries.values()),this.iterations);

		// Above clusters are for preprocessed summary. Find original summary of
		// each preprocessed summary and Map the cluster number.
		for (Map.Entry<String, String> entrySet : preprocessedSummaries
				.entrySet())
			entrySet.setValue(String.valueOf(clusters.get(entrySet.getValue())));

		return preprocessedSummaries;
	}
	
	/**
	 * This method takes a list of summaries. It preprocesses each summary, them
	 * creates a Map of original summary and preprocessed summary.
	 * 
	 * @param summaryList
	 * @return
	 */
	private Map<String, String> createClusteringInput(List<String> summaryList) {
		Map<String, String> summaryMap = new HashMap<String, String>();
		for (String summary : summaryList) {
			// Remove all tabs and other escape characters from original
			// summary.
			for (String escapeChar : Constants.ESCAPE_CHARACTERS)
				summary = summary.replaceAll(Pattern.quote(escapeChar), "");
			summaryMap.put(summary.trim(), null);
		}
		// Now preprocess each summary.
		return this.preprocessing.removeStopWords(summaryMap);
	}
	
	
	
	/**
	 * This function computes actual number of clusters formed.
	 * @param clusters
	 * @return
	 */
	private int actualDistinctClusters(Map<String,Integer> clusters){
		int[] arr=new int[this.K];
		for(Map.Entry<String, Integer> entrySet:clusters.entrySet())
			arr[entrySet.getValue()] +=1;
		
		//Now find how many clusters have more than zero members.
		int count=0;
		for(int i=0;i<arr.length;i++){
			if(arr[i]>0)
				count++;
		}
		
		return count;
	}

	public static void main(String a[]) {
		String[] documents = { "java programming language",
				"python programming", "nlp python", "programming scala",
				"high price", "low price of sales", "sales manager",
				"retail price", "java sales", "python high price",
				"scala manager" };

		Map<String, String> preprocessedSummaries =new Clustering(10, SimilarityMetric.EUCLIDIAN).runClustering(Arrays
				.asList(documents));
		System.out.println(preprocessedSummaries);

	}
}
