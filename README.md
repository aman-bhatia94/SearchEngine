# SearchEngine
A text based search Engine(inspired from Lucene)

The entire project is composed of four sub projects

- SearchEngine
- InvertedIndex
- SearchScore
- Ranking

### SearchEngine

- In this project, the huge collection of files is transformed and tokenized. In order to do this, we use a porter stemming
library and filter the files using stopwords. Doing this transformation step, makes it easier to create inverted indexes.

### Inverted Indexes

- In this project we look at each word from the transformed files and create an entry into the inverted index file. 
In order to improve search time and index lookup, I used sharding, and stored all indexes that start with 'a' in a file a,
those that start with 'b' in a file b and so on and so forth. Each entry in the index files comprises of the term(the word) along with its postings
Each posting is a colon separated list of a particular document's attributes

Example apple 1:5:3,4,5;2:6:4,7,8

This means that the word apple occurs in document 1. It occurs 5 times and at positions 3,4,5
Similary apple also occurs in document 2, 6 times at positions 4,7 and 8.


### SearchScore and Ranking

In the SearchScore project I have implemented a simple scoring algorithm, which ranks relevant documents on the basis of the term frequency
and position score. The user is asked to input a query. We filter the input query using our transformation procedure. Now for all the
words or terms in the filter query, we get the term frequency for each document and also add to it the psoition_score in that document to generate a final score.
The position score is the sum of inverse distances between two consecutive query words in a document. We inverse the distance, because we assume
that if the terms are near(shorter distance, the document should be more relevant). To make this assumption work, we inverse the distance
so that a shorter distance would generate a bigger score. Once all this is done, we display the top 10 results to the user

The Ranking project improves the scoring algorithm by using Cosine Similarity.
