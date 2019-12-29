import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class InvertedIndex {

    private HashMap<String,List<String>> postings[];

    InvertedIndex(){
        postings = new HashMap[26];

        for(int i = 0; i < 26; i++){
            postings[i] = new HashMap<>();
        }
    }


    public void createIndex(String path){
        File file = new File(path);
        File[] files = file.listFiles();

        for(File fileTemp : files ){
            //process
            System.out.println("Processing started for file:"+fileTemp.getName());
            process(fileTemp);
        }
        createInvertedIndex();
    }

    private void process(File file) {

        //create an array list to store all words in this file
        ArrayList<String> words = new ArrayList<>();

        try {

            Scanner scanner = new Scanner(file);
            while(scanner.hasNext()){
                String word = scanner.next();
                if(!Character.isDigit(word.charAt(0))){
                    words.add(word);
                }


            }
            System.out.println("Word list for file: "+file.getName()+" created");
            //now all words in the file have been added to arraylist
            //for each word in array list we will make a string of positions
            HashMap<String,List<String>> tempMap = new HashMap<>();

            //words from this file being put into a temp hashMap
            for(int i = 0; i < words.size(); i++){

                //check if word is present in hashMap
                if(!tempMap.containsKey(words.get(i))){
                    List<String> tempPositions = new ArrayList<>();
                    tempPositions.add(String.valueOf(i+1));
                    tempMap.put(words.get(i),tempPositions);
                }
                else {
                    List<String> tempPostn = tempMap.get(words.get(i));
                    tempPostn.add(String.valueOf(i + 1));
                    tempMap.put(words.get(i), tempPostn);
                }
            }

            //now iterate through this map and call create map index

            Iterator hashIterator = tempMap.entrySet().iterator();
            while (hashIterator.hasNext()) {
                Map.Entry mapElement = (Map.Entry)hashIterator.next();
                ArrayList<String> positions = (ArrayList<String>) mapElement.getValue();
                String term = (String) mapElement.getKey();
                createMapIndex(file, term, positions);
            }

            /*for(int i = 0; i < words.size(); i++){
                //ArrayList<String> positions = new ArrayList<>();
                if(!tempMap.containsKey(words.get(i))){
                    List<String> tempPositions = new ArrayList<>();
                    tempMap.put(words.get(i),tempPositions);
                }
                for(int j = 0; j < words.size(); j++){

                    if(words.get(i).equals(words.get(j))){
                        //positions.add(String.valueOf(j+1));

                    }

                }
                //now for each words its positions string is made
                //so we will insert this entry into a particular hashmap
                createMapIndex(file, words.get(i), positions);
            }*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void createInvertedIndex() {

        for(int i = 0; i < 26; i++){
            System.out.println("Writing the invertedIndexes........");
            FileWriter outputFile = null;

            char fileName = (char)(i+97);

            try{
                //outputFile = new FileWriter("/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/inv-index/"+fileName+".txt",true);
                //outputFile = new FileWriter("src/testIndex/"+fileName+".txt",true);
                outputFile = new FileWriter("/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/inv-index/"+fileName+".txt");

                ArrayList<String> sortedKeys = new ArrayList<>(postings[i].keySet());
                Collections.sort(sortedKeys);

                for(String term : sortedKeys){

                    List<String> payloadList = postings[i].get(term);
                    //create a semicolon separated payload list

                    StringBuilder sb = new StringBuilder();
                    for(int j = 0; j < payloadList.size(); j++){
                        //System.out.println("index j value now: "+j);
                        sb.append(payloadList.get(j));

                        if(j < payloadList.size() - 1){
                            sb.append(";");
                        }
                    }

                    //create a final semicolon separted payload list string
                    String payLoadListString = sb.toString();
                    String entryInFile = term+" "+payLoadListString;
                    outputFile.append(entryInFile);
                    outputFile.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    outputFile.flush();
                    outputFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

    }

    private void createMapIndex(File file, String term, ArrayList<String> positions) {

        String documentName = file.getName().replace(".txt","");
        int frequency = positions.size();

        //make positions list into an a comma separated string
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < positions.size(); i++){

            sb.append(positions.get(i));
            if(i < positions.size() - 1){
                sb.append(",");
            }

        }
        String occurences = sb.toString();

        //create the payload string
        String payload = documentName+":"+String.valueOf(frequency)+":"+occurences;
        int mapNumber = (int)term.charAt(0) - 97;

        //check if this map already contains this entry, then will add to the List<String>
        //else create a new List<String>

        if(postings[mapNumber].containsKey(term)){

            // write logic to append to its list
            List<String> payLoadList = postings[mapNumber].get(term);

            payLoadList.add(payload);
            postings[mapNumber].put(term,payLoadList);


        }
        else{
            List<String> payLoadList = new ArrayList<>();
            payLoadList.add(payload);

            //put this entry into the map
            postings[mapNumber].put(term,payLoadList);

        }

    }


    public static void main(String[] args) {


        InvertedIndex invertedIndex = new InvertedIndex();
        String directory = "/Users/amanbhatia/IdeaProjects/Information Retrieval/searchengine/src/ir_files/input-transform";
        //String directory = "src/testFiles";
        invertedIndex.createIndex(directory);

    }

}
