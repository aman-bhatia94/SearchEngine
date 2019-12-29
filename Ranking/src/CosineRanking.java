import opennlp.tools.stemmer.PorterStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CosineRanking {

    private ArrayList<String> stopWords;
    private List<String> queryWords;
    private HashMap<String,Double> cosineScoreNum;  //holds the final numerator values of cosine formula for all docs
    private HashMap<String,Double> cosineScoreDenom;  //holds the final numerator values of cosine formula for all docs
    private HashMap<String,Double> finalCosineScore;
    private HashMap<String,List<String>> docPositions;
    private HashMap<String,HashMap<String,Double>> termNumMap; //used to store numberators for dij for each term j and doc i
    private HashMap<String,Double> termDenomMap; ////used to store denoms for dij
    private HashMap<String,Double> positionScoreMap; // used to store positional scores for docs
    private int weight;
    private int sigmaWeight;
    private double numDocs;

    CosineRanking(String docsPath){
        queryWords = new ArrayList<>();
        cosineScoreNum = new HashMap<>();
        cosineScoreDenom = new HashMap<>();
        docPositions = new HashMap<>();
        finalCosineScore = new HashMap<>();
        positionScoreMap = new HashMap<>();
        termNumMap = new HashMap<>();
        termDenomMap = new HashMap<>();
        weight = 1;
        sigmaWeight = 0; //change to number_of_query_terms * weight

        //initialize stopWords
        stopWords = new ArrayList<>();
        int i = 0;
        File docsFiles = new File(docsPath);
        File[] allDocs = docsFiles.listFiles();
        numDocs = allDocs.length;
        File file = new File("src/stopwords.txt");
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while(scanner.hasNextLine()){
                stopWords.add(scanner.nextLine());

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    public void filterQuery(String query){

        //Now filter the query using stopwords and stemming
        PorterStemmer porterStemmer = new PorterStemmer();

        String[] tokens = query.split("[^a-zA-Z0-9]+");

        for (String token : tokens) {
            if(!stopWords.contains(token.toLowerCase())) {
                String stemToken = porterStemmer.stem(token.toLowerCase());
                queryWords.add(stemToken);
            }
        }

        sigmaWeight = queryWords.size()*weight;
        System.out.println("Filtered query words: "+queryWords.toString());

    }

    public void cosineScoreCount(String path) {

        for (String term : queryWords) {
            double numK = 0; //number of documents in which this term occurs, will be set below, used to compute idf
            //Array list storing the postings for this particular term
            //List<String> postings = new ArrayList<>();
            char ch = term.charAt(0);
            File indexFile = new File(path + "/" + ch + ".txt");
            Scanner scanner;
            try {
                scanner = new Scanner(indexFile);
                //search for this term in the file
                while (scanner.hasNextLine()) {
                    String[] index = scanner.nextLine().split(" ");
                    //if the term equals this index term, add to postings list
                    if (term.equals(index[0])) {
                        //each string[] item is one posting
                        String[] postings = index[1].split(";");
                        numK = postings.length;
                        //now we can also get idf term
                        double idf = Math.log(numDocs/numK);
                        //check for NaN

                        if(Double.isNaN(Math.log(numDocs/numK))){
                            System.out.println("numdocs and numK"+numDocs+"  "+numK);
                        }

                        for(String posting : postings){
                            //for each document
                            String[] attributes =  posting.split(":");

                            double fik = Double.parseDouble(attributes[1]); //getting the frequency of this term in this doc

                            //we have all the terms to calculate the dij num for this doc for this term
                            //we do that below
                            double numerator = 0;
                            if(fik > 0){
                                numerator = (Math.log(fik)+1.0)*idf;
                            }
                            if(Double.isNaN((Math.log(fik)+1.0)*idf)){
                                System.out.println("posting: "+posting);
                            }
                            String document = attributes[0];
                            //now we put this entry in the hashmap for this particular term, for this particular document

                            if(termNumMap.containsKey(term)){

                                HashMap<String,Double> innerMap = termNumMap.get(term);
                                innerMap.put(document,numerator);
                                termNumMap.put(term,innerMap);

                            }
                            else{
                                HashMap<String,Double> innerMap = new HashMap<>();
                                innerMap.put(document,numerator);
                                termNumMap.put(term,innerMap);
                            }

                            //we also add to our denominator map,
                            //the value to add to denominator map is square of the nums, we will take care of the root later
                            //since we our adding this value to previous values in the denom map, our sigma gets taken care of
                            double denominator = numerator;
                            if(denominator != 0) {

                                denominator = Math.pow(denominator, 2);
                            }

                            if(Double.isNaN((Math.log(fik)+1.0)*idf)){
                                System.out.println(posting);
                            }

                            if(termDenomMap.containsKey(document)){

                                double denomPresent = termDenomMap.get(document);
                                double denomUpdated = denomPresent + denominator;
                                termDenomMap.put(document,denomUpdated);
                            }
                            else{
                                termDenomMap.put(document,denominator);
                            }

                            //position map for all docs
                            if(docPositions.containsKey(attributes[0])){

                                List<String> allTermPositions = docPositions.get(attributes[0]);
                                allTermPositions.add(attributes[2]); //adding new comma separated positions, for a term
                                docPositions.put(attributes[0],allTermPositions);

                            }
                            else{
                                List<String> allTermPositions = new ArrayList<>();
                                allTermPositions.add(attributes[2]);
                                docPositions.put(attributes[0],allTermPositions);
                            }

                            //initialize the positionscore map to 0 for all docs
                            positionScoreMap.put(attributes[0],0.0);

                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void rootAllDenoms(){

        Iterator denomIterator = termDenomMap.entrySet().iterator();

        while(denomIterator.hasNext()){

            Map.Entry mapElement = (Map.Entry)denomIterator.next();

            String key = (String) mapElement.getKey();
            double value = (double) mapElement.getValue();

            double newDenom = Math.sqrt(value);

            if(Double.isNaN(Math.sqrt(value))){
                System.out.println("key: "+key+"  value:"+newDenom);
            }
            termDenomMap.put(key,newDenom);

        }

    }

    //method that divides the numerators and denominators and updates the numerator map
    //after this method the termNumMap has final dij values for all documents for all terms in query
    public void calculateFinalDij(){

        Iterator iterator = termNumMap.entrySet().iterator();

        while(iterator.hasNext()){

            Map.Entry mapElement = (Map.Entry)iterator.next();
            //this loop is for this term
            String key = (String) mapElement.getKey();
            HashMap<String,Double> innerMap = termNumMap.get(key);

            Iterator innerIterator = innerMap.entrySet().iterator();

            while (innerIterator.hasNext()){

                Map.Entry mapElementInner = (Map.Entry)innerIterator.next();
                //this is for all documents for this term

                String keyInner = (String) mapElementInner.getKey();

                double presentNumValue = innerMap.get(keyInner);

                double denomForThisTerm = termDenomMap.get(keyInner);

                double updatedDij = presentNumValue / denomForThisTerm;

                innerMap.put(keyInner,updatedDij);


            }
            termNumMap.put(key,innerMap); // update the outer map also for this term

        }

    }

    public void updateCosineScores(){

        Iterator termIterator = termNumMap.entrySet().iterator();

        while(termIterator.hasNext()){

            //for each term
            Map.Entry termMapElement = (Map.Entry)termIterator.next();

            String key = (String)termMapElement.getKey(); //each key is a term

            HashMap<String,Double> innerMap = termNumMap.get(key);
            Iterator innerIterator = innerMap.entrySet().iterator();

            //For every document keep updating cosine num and denom

            while(innerIterator.hasNext()){

                Map.Entry mapElementInner = (Map.Entry)innerIterator.next();

                String docKey = (String) mapElementInner.getKey();

                double numValue = innerMap.get(docKey);

                if(cosineScoreNum.containsKey(docKey)){
                    //update the score
                    double previousTermDij = cosineScoreNum.get(docKey);

                    double newSigmaDij = previousTermDij + numValue;

                    cosineScoreNum.put(docKey,newSigmaDij);

                }
                else{

                    //else put a new value
                    cosineScoreNum.put(docKey,numValue);


                }

                //do it for cosine denom also

                if(cosineScoreDenom.containsKey(docKey)){

                    //update

                    double previousTermDijSquared = cosineScoreDenom.get(docKey);
                    double currentDijSquared = Math.pow(numValue,2.0);
                    if(Double.isNaN(Math.pow(numValue,2.0))){
                        System.out.println("here in 290");
                    }

                    double updatedDijSquared = previousTermDijSquared + currentDijSquared;

                    cosineScoreDenom.put(docKey,updatedDijSquared);

                }
                else{
                    //put

                    double currentDijSquared = Math.pow(numValue,2.0);
                    if(Double.isNaN(Math.pow(numValue,2.0))){
                        System.out.println("here in 303");
                    }
                    cosineScoreDenom.put(docKey,currentDijSquared);

                }
            }
        }
    }

    public void multiplyAndRootCosineDenom(){

        Iterator iterator = cosineScoreDenom.entrySet().iterator();

        while(iterator.hasNext()){

            Map.Entry mapElement = (Map.Entry)iterator.next();
            String key = (String) mapElement.getKey();

            double currentDenom = cosineScoreDenom.get(key);

            double multSigmaDenom = currentDenom*sigmaWeight;

            double rootedDenom = Math.sqrt(multSigmaDenom);
            if(Double.isNaN(Math.sqrt(multSigmaDenom))){
                System.out.println("error here in 328");
            }

            cosineScoreDenom.put(key,rootedDenom);

        }

    }

    public void makeFinalCosineScore(){
        Iterator iterator = cosineScoreNum.entrySet().iterator();

        while (iterator.hasNext()){

            Map.Entry mapElement = (Map.Entry)iterator.next();

            String key = (String)mapElement.getKey(); //each key is a term

            double cosineNum = cosineScoreNum.get(key);
            double cosineDenom = cosineScoreDenom.get(key);

            double cosineScore = cosineNum/cosineDenom;

            finalCosineScore.put(key,cosineScore);

        }
    }

    //this will set the position score, to normalize it, we need to divide it by (number of terms in query words -1)
    public void setPositionScore() {

        //All entries in docPositions map contain a list of strings, each of which describes positions occuring for one term
        //so for each map entry, we will iterate through those strings and get minimum difference

        for (String doc : docPositions.keySet()) {

            List<String> allTermPos = docPositions.get(doc);

            //iterate till last but one, since we want smallest difference between i and i + 1
            for (int i = 0; i < allTermPos.size() - 1; i++) {

                String[] term1 = allTermPos.get(i).split(",");
                String[] term2 = allTermPos.get(i + 1).split(",");

                //term 1 and term 2 contain two comma separated lists, we need to find minimum difference between them

                int shortestDistance = findSmallestDifference(term1, term2);

                double inverseShortDistance = (double) (1.0 / (double) shortestDistance);

                /*double score = scoreMap.get(doc);
                score = score + inverseShortDistance;
                scoreMap.put(doc, score);*/

                if(positionScoreMap.containsKey(doc)){

                    double positionScore = positionScoreMap.get(doc);
                    double updatedScore = positionScore + inverseShortDistance;
                    positionScoreMap.put(doc,updatedScore);

                }
                else{

                    positionScoreMap.put(doc,inverseShortDistance);

                }

            }


        }
    }

    private int findSmallestDifference(String[] term1, String[] term2) {

        int[] nums1 = new int[term1.length];
        int[] nums2 = new int[term2.length];

        //converting to int arrays
        for(int i = 0; i < term1.length; i ++){
            nums1[i] = Integer.parseInt(term1[i]);
        }
        for(int i = 0; i < term2.length; i ++){
            nums2[i] = Integer.parseInt(term2[i]);
        }

        Arrays.sort(nums1);
        Arrays.sort(nums2);

        //get two integers to keep track of nums

        int i = 0;
        int j = 0;

        int shortestDistance = Integer.MAX_VALUE;

        while(i < nums1.length && j < nums2.length){


            if(Math.abs(nums1[i] - nums2[j]) < shortestDistance){
                shortestDistance = Math.abs(nums1[i] - nums2[j]);
                if(Double.isNaN(Math.abs(nums1[i] - nums2[j]))){
                    System.out.println("error here in 430");
                }
            }

            if(nums1[i] < nums2[j]){
                i++;
            }
            else{
                j++;
            }
        }
        return shortestDistance;

    }

    public void normalizePositionScoreAndAddToCosineScore(){

        Iterator iterator = positionScoreMap.entrySet().iterator();

        while (iterator.hasNext()){

            Map.Entry mapElement = (Map.Entry)iterator.next();
            String key = (String) mapElement.getKey();

            double docPositionScore = positionScoreMap.get(key);

            double newDocPositionScore;
            if(queryWords.size() == 1){
                newDocPositionScore = docPositionScore;
            }
            else {
                newDocPositionScore = docPositionScore / (queryWords.size() - 1);
            }

            positionScoreMap.put(key,newDocPositionScore);

            //add this position score to the final cosine map also

            double cosineScore = finalCosineScore.get(key);

            double finalScore = cosineScore + newDocPositionScore;


            finalCosineScore.put(key,finalScore);


        }

    }

    public void displayTopResults(String directory){

        //sort the map using top ten scores

        LinkedHashMap<String,Double> topTenMap = new LinkedHashMap<>();


        //source: https://howtodoinjava.com/sort/java-sort-map-by-values/ for the following code snippet
        finalCosineScore.entrySet()
                .stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(entry -> topTenMap.put(entry.getKey(),entry.getValue()));


        int counter = 1; //to get top 10 results

        Set<String> keys = topTenMap.keySet();

        for(String key : keys){

            if(counter == 11){
                break;
            }
            //String document = key;
            //String docPath = directory;

            //System.out.println(counter+".  "+directory+"/"+key+ " score: "+topTenMap.get(key));
            System.out.println(counter+".  "+directory+"/"+key);
            counter++;
        }
    }



    public static void main(String[] args) {

        String directory = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/inv-index";
        //String directory = "src/test";
        String docsPath = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/input-transform";
        String finalPrintDocsPath = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/input-transform";
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your query");
        //get the query from the user
        String query = scanner.nextLine();



        CosineRanking ranking = new CosineRanking(docsPath);
        //transform the query
        ranking.filterQuery(query);
        ranking.cosineScoreCount(directory);
        ranking.rootAllDenoms();
        ranking.calculateFinalDij();
        ranking.updateCosineScores();
        ranking.multiplyAndRootCosineDenom();
        ranking.makeFinalCosineScore();
        ranking.setPositionScore();
        ranking.normalizePositionScoreAndAddToCosineScore();
        ranking.displayTopResults(finalPrintDocsPath);

    }




}
