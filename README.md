# GrayMatter
This is a Machine Learning API focusing on Machine Learning Algorithms on Java.

Usage:

int iterations=10;
Clustering clusteringObj=new Clustering(iterations,SimilarityMetric.EUCLIDIAN);

List<String> data=new ArrayList<String>();
data.add("Statement-1");
data.add("Statement-2");
...
...
data.add("Statement-N");

Map<String, String> clusters=clustering.runClustering(data);

Here the values of Map are in the form <Statement,Cluster_number>

