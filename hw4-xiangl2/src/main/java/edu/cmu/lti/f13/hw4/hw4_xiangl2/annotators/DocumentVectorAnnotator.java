package edu.cmu.lti.f13.hw4.hw4_xiangl2.annotators;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.cmu.lti.f13.hw4.hw4_xiangl2.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_xiangl2.typesystems.Token;

/**
 * Analysis engine which generate token list for the document.
 * <p>
 * This analysis engine divides the text string of the document into tokens. It then counts the
 * occurrence times of the tokens, and stores the result in the token list.
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  /**
   * The major process for Document Vector Annotator.
   * <p>
   * This method does tokenize for the text string using Stanford CoreNLP library. It then counts
   * the occurrence times of each token, and stores the text string and frequency of the tokens in
   * the token list.
   */
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
    Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(docText));

    while (tokenizer.hasNext()) {
      Word word = tokenizer.next();
      String text = docText.substring(word.beginPosition(), word.endPosition());
      if (map.containsKey(text)) {
        map.put(text, map.get(text) + 1);
      } else {
        map.put(text, 1);
      }
    }

    NonEmptyFSList head = new NonEmptyFSList(jcas);
    NonEmptyFSList list = head;
    Iterator iter = map.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      Token token = new Token(jcas);
      token.setText((String) entry.getKey());
      token.setFrequency((Integer) entry.getValue());
      token.addToIndexes();
      list.setHead(token);
      if (iter.hasNext()) {
        NonEmptyFSList nextList = new NonEmptyFSList(jcas);
        list.setTail(nextList);
        list = nextList;
      } else {
        list.setTail(new EmptyFSList(jcas));
      }
    }
    doc.setTokenList(head);
  }

}
