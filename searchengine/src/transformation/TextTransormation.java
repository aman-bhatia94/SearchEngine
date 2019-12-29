package transformation;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import opennlp.tools.stemmer.*;



public class TextTransormation {


    private ArrayList<String> stopWords;
    TextTransormation(){
        stopWords = new ArrayList<>();
        int i = 0;
        //initialize stopWords
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

    //this function will get all files from the input-files
    //unzip and store them in ir_unzipped
    //tokenize will use that to carry out tokenization
    public void getFiles(String directoryName){

        File directory = new File(directoryName);
        File[] filesList = directory.listFiles();

        if(filesList != null){
            for(File file : filesList){
                if(file.isFile()){
                    //inputFiles.add(file);
                    File outputFile = new File("src/ir_unzipped/"+file.getName().replace(".zip",".txt"));
                    decompress(file,outputFile);

                }
                else if(file.isDirectory()){
                    getFiles(file.getAbsolutePath());
                }
            }
        }

    }


    //method to decompress the file
    private void decompress(File file,File outputFile){
        final int BUFFER = 2048;
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new
                    FileInputStream(file);
            ZipInputStream zis = new
                    ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while((entry = zis.getNextEntry()) != null) {
                System.out.println("Extracting: " +entry);
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new
                        FileOutputStream(outputFile);
                dest = new
                        BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER))
                        != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*
    private void tokenize(){
        //Scanner scanner = new Scanner(file);
        //String[] tokens = scanner.nextLine().split("[^a-zA-Z0-9]+");
        //String[] tokens = "I, am, Aman  Bhatia 9999 -+cc , nope".split("[^a-zA-Z0-9]+");
        PorterStemmer porterStemmer = new PorterStemmer();
        String input = "Document will describe marketing strategies carried out by U.S. companies for their agricultural \n" +
                "chemicals, report predictions for market share of such chemicals, or report market statistics for \n" +
                "agrochemicals, pesticide, herbicide, fungicide, insecticide, fertilizer, predicted sales, market share, \n" +
                "stimulate demand, price cut, volume of sales.";

        String[] tokens = input.split("[^a-zA-Z0-9]+");
        StringBuilder combined = new StringBuilder();
        for (String token : tokens) {
            if(!stopWords.contains(token)) {
                combined.append(token + " ");
            }
        }
        String tokenedString = combined.toString().trim();
        //stem the tokenedString and append into the file

        String stemmed = porterStemmer.stem(tokenedString);
        System.out.println(stemmed);
    }*/


    private void tokenize(String path,String outputPath) throws FileNotFoundException {

        PorterStemmer porterStemmer = new PorterStemmer();
        File directory = new File(path);
        File[] filesToTransform = directory.listFiles();

        for(File file: filesToTransform) {
            FileWriter outputFile = null;
            try {
                //outputFile = new FileWriter("src/ir_files/input-transform/"+file.getName(),true);
                outputFile = new FileWriter("src/ir_files/input-transform/"+file.getName());

            } catch (IOException e) {
                e.printStackTrace();
            }
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String[] tokens = scanner.nextLine().split("[^a-zA-Z0-9]+");
                //String[] tokens = "I, am, Aman  Bhatia 9999 -+cc , nope".split("[^a-zA-Z0-9]+");
                StringBuilder combined = new StringBuilder();
                for (String token : tokens) {
                    if(!stopWords.contains(token.toLowerCase())) {
                        String stemToken = porterStemmer.stem(token.toLowerCase());
                        combined.append(stemToken+" ");
                    }
                }
                String tokenedString = combined.toString();
                //stem the tokenedString and append into the file

                //String stemmed = porterStemmer.stem(tokenedString);
                try {
                    outputFile.append(tokenedString);
                    //outputFile.append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                outputFile.flush();
                outputFile.close();
                System.out.println("Text Transformation in progress....");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {


        String directoryPath = "src/ir_files/input-files/aleph.gutenberg.org/1/";
        //String directoryPath = "src/testing";

        TextTransormation textTransormation = new TextTransormation();
        textTransormation.getFiles(directoryPath);

        String textTransformInputPath = "src/ir_unzipped/";
        String outputPath = "src/ir_files/input-transform/";

        try {
            textTransormation.tokenize(textTransformInputPath,outputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }




    }
}
