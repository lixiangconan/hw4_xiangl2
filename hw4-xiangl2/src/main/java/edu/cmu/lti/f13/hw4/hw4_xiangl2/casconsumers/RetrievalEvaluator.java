package edu.cmu.lti.f13.hw4.hw4_xiangl2.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_xiangl2.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_xiangl2.typesystems.Token;

/**
 * Analysis engine which does vector space retrieval using cosine similarity.
 * <p>
 * This analysis engine measures cosine similarity between query sentence and its candidate result.
 * Then it ranks the results according to the similarity. Finally it calculates the MRR (Mean
 * Reciprocal Rank) for all queries.
 */
public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** text in each document **/
  public ArrayList<String> textList;

  /** global token dictionary **/
  public HashMap<String, Integer> dictionary;

  /** token list for each document **/
  public ArrayList<HashMap<String, Integer>> tokenList;

  /**
   * The initialize method.
   */
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    textList = new ArrayList<String>();

    tokenList = new ArrayList<HashMap<String, Integer>>();

    dictionary = new HashMap<String, Integer>();

  }

  /**
   * The process method for each document.
   * <p>
   * For each document, this method extracts and stores the results of previous annotators, such
   * like query ID, relevant value text string and token list. It also generates the global
   * dictionary.
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      
      Document doc = (Document) it.next();

      FSList fsTokenList = doc.getTokenList();

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      textList.add(doc.getText());

      HashMap<String, Integer> tList = new HashMap<String, Integer>();
      int i = 0;
      while (true) {
        Token token = null;
        try {
          token = (Token) doc.getTokenList().getNthElement(i);
          tList.put(token.getText(), token.getFrequency());
          if (!dictionary.containsKey(token.getText())) {
            dictionary.put(token.getText(), dictionary.size());
          }
        } catch (Exception e) {
          break;
        }
        i++;
      }
      tokenList.add(tList);
    }

  }

  /**
   * The process method which does Vector Space Retrieval.
   * <p>
   * After each document being processed, this method transforms their token lists into feature
   * vectors. Then it uses cosine similarity to measure the distance between two documents, and
   * ranks the query results using the cosine similarity. Finally, it calculates MRR (Mean
   * Reciprocal Rank) for all queries.
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    HashMap<Integer, Integer> query = new HashMap<Integer, Integer>();
    for (int i = 0; i < qIdList.size(); i++) {
      if (relList.get(i) == 99) {
        query.put(qIdList.get(i), i);
      }
    }

    int queryNum = query.size();
    int sentenceNum = qIdList.size();
    int[] rank = new int[sentenceNum];
    double[] score = new double[sentenceNum];
    int featureNum = dictionary.size();
    int[][] featureVector = new int[sentenceNum][featureNum];

    for (int i = 0; i < qIdList.size(); i++) {
      Iterator tokeniter = tokenList.get(i).entrySet().iterator();
      while (tokeniter.hasNext()) {
        Map.Entry entry = (Map.Entry) tokeniter.next();
        featureVector[i][dictionary.get((String) entry.getKey())] = (Integer) entry.getValue();
      }
    }

    // compute the cosine similarity measure
    Iterator iter = query.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      int queryID = (Integer) entry.getKey();
      int queryPos = (Integer) entry.getValue();
      for (int i = 0; i < qIdList.size(); i++) {
        if (qIdList.get(i) == queryID) {
          score[i] = computeCosineSimilarity(featureVector[queryPos], featureVector[i]);
        }
      }
    }

    // compute the rank of retrieved sentences
    iter = query.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      int queryID = (Integer) entry.getKey();
      ArrayList<Integer> result = new ArrayList<Integer>();
      for (int i = 0; i < qIdList.size(); i++) {
        if (qIdList.get(i) == queryID && relList.get(i) != 99) {
          result.add(i);
        }
      }
      for (int i = 0; i < result.size(); i++) {
        for (int j = i + 1; j < result.size(); j++) {
          if (score[result.get(i)] < score[result.get(j)]) {
            int t = result.get(i);
            result.set(i, result.get(j));
            result.set(j, t);
          }
        }
      }
      for (int i = 0; i < result.size(); i++) {
        rank[result.get(i)] = i + 1;
      }
    }

    // output query results
    System.out.println("Retrival using cosine similarity:");
    for (int i = 0; i < qIdList.size(); i++) {
      if (relList.get(i) == 1)
        System.out.println("Score: " + score[i] + "   rank=" + rank[i] + "   rel=" + relList.get(i)
                + "  qid=" + qIdList.get(i) + " " + textList.get(i));
    }

    // compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr(queryNum, rank);
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
    System.out.println();
  }

  // Calculate the cosine similarity between two sentences.
  private double computeCosineSimilarity(int[] featureVector1, int[] featureVector2) {
    double cosine_similarity = 0.0;
    double normVector1 = 0.0, normVector2 = 0.0;
    int featureSize = featureVector1.length;

    for (int i = 0; i < featureSize; i++) {
      cosine_similarity = cosine_similarity + featureVector1[i] * featureVector2[i];
      normVector1 = normVector1 + featureVector1[i] * featureVector1[i];
      normVector2 = normVector2 + featureVector2[i] * featureVector2[i];
    }

    cosine_similarity = cosine_similarity / (Math.sqrt(normVector1) * Math.sqrt(normVector2));

    return cosine_similarity;
  }

  // Calculate the Mean Reciprocal Rank for all queries.
  private double compute_mrr(int queryNum, int[] rank) {
    double metric_mrr = 0.0;

    for (int i = 0; i < relList.size(); i++) {
      if (relList.get(i) == 1) {
        metric_mrr = metric_mrr + 1.0 / rank[i];
      }
    }

    metric_mrr = metric_mrr / queryNum;

    return metric_mrr;
  }

}
