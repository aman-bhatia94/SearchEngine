import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import opennlp.tools.stemmer.*;

public class Score {

    private ArrayList<String> stopWords;
    private List<String> queryWords;
    private HashMap<String,Double> scoreMap;
    private HashMap<String,List<String>> docPositions;

    Score(){
        queryWords = new ArrayList<>();
        scoreMap = new HashMap<>();
        docPositions = new HashMap<>();

        //initialize stopWords
        stopWords = new ArrayList<>();
        int i = 0;

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
        System.out.println("Filtered query words: "+queryWords.toString());

    }

    //This method will set the score counts in scoreMap, the other method will update the scores with score positions as well.
    public void frequencyScoreCount(String path){

        for(String term : queryWords){
            //Array list storing the postings for this particular term
            //List<String> postings = new ArrayList<>();
            char ch = term.charAt(0);
            File indexFile = new File(path+"/"+ch+".txt");
            Scanner scanner;
            try {
                scanner = new Scanner(indexFile);
                //search for this term in the file
                while(scanner.hasNextLine()){
                    String[] index = scanner.nextLine().split(" ");
                    //if the term equals this index term, add to postings list
                    if(term.equals(index[0])){

                        //each string[] item is one posting
                        String[] postings = index[1].split(";");

                        for(String posting : postings){

                            String[] docScore = posting.split(":");
                            if(scoreMap.containsKey(docScore[0])){

                                double count = scoreMap.get(docScore[0]);
                                count = count + Integer.parseInt(docScore[1]);
                                scoreMap.put(docScore[0],count);
                            }
                            else{

                                double count = Integer.parseInt(docScore[1]);
                                scoreMap.put(docScore[0],count);
                            }
                            //we use the postings[] again to create a doc,positions map for all docs

                            if(docPositions.containsKey(docScore[0])){

                                List<String> allTermPositions = docPositions.get(docScore[0]);
                                allTermPositions.add(docScore[2]); //adding new comma separated positions, for a term
                                docPositions.put(docScore[0],allTermPositions);

                            }
                            else{
                                List<String> allTermPositions = new ArrayList<>();
                                allTermPositions.add(docScore[2]);
                                docPositions.put(docScore[0],allTermPositions);
                            }

                        }


                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    public void setPositionScore(){

        //All entries in docPositions map contain a list of strings, each of which describes positions occuring for one term
        //so for each map entry, we will iterate through those strings and get minimum difference


        for(String doc : docPositions.keySet()){

            List<String> allTermPos = docPositions.get(doc);

            //iterate till last but one, since we want smallest difference between i and i + 1
            for(int i = 0; i < allTermPos.size() - 1; i++){

                String[] term1 = allTermPos.get(i).split(",");
                String[] term2 = allTermPos.get(i+1).split(",");

                //term 1 and term 2 contain two comma separated lists, we need to find minimum difference between them

                int shortestDistance = findSmallestDifference(term1,term2);

                double inverseShortDistance = (double)(1.0/(double)shortestDistance);
                //System.out.println("doc: "+doc+" "+inverseShortDistance);

                double score = scoreMap.get(doc);
                score = score + inverseShortDistance;
                scoreMap.put(doc,score);

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

    public void displayTopResults(String directory){

        //sort the map using top ten scores

        LinkedHashMap<String,Double> topTenMap = new LinkedHashMap<>();


        //source: https://howtodoinjava.com/sort/java-sort-map-by-values/ for the following code snippet
        scoreMap.entrySet()
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


        Score score = new Score();
        String directory = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/inv-index";
        String topTenPath = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/input-transform";
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your query");
        //get the query from the user
        String query = scanner.nextLine();

        score.filterQuery(query);
        score.frequencyScoreCount(directory);
        score.setPositionScore();
        score.displayTopResults(topTenPath);




    }
}
