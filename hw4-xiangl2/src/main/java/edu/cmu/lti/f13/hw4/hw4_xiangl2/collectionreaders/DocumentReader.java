package edu.cmu.lti.f13.hw4.hw4_xiangl2.collectionreaders;

import java.util.ArrayList;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.cmu.lti.f13.hw4.hw4_xiangl2.typesystems.Document;

/**
 * Analysis engine which reads documents and extract features.
 * <p>
 * This analysis engine reads documents which only contain one sentence. It then extracts features
 * such like query ID, relevant value and text string and generates annotations.
 */
public class DocumentReader extends JCasAnnotator_ImplBase {

  /**
   * The major process for Document Reader.
   * <p>
   * This method reads the content of the document. It then divides the content into three part:
   * query ID, relevant value and text string. Finally, it stores these information in a Document
   * Annotation.
   */
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    // reading sentence from the CAS
    String sLine = jcas.getDocumentText();

    ArrayList<String> docInfo = parseDataLine(sLine);

    // This is to make sure that parsing done properly and
    // minimal data for rel,qid,text are available to proceed
    if (docInfo.size() < 3) {
      System.err.println("Not enough information in the line");
      return;
    }

    int rel = Integer.parseInt(docInfo.get(0));
    int qid = Integer.parseInt(docInfo.get(1));
    String txt = docInfo.get(2);

    Document doc = new Document(jcas);
    doc.setBegin(0);
    doc.setEnd(sLine.length());
    doc.setText(txt);
    doc.setQueryID(qid);
    doc.setRelevanceValue(rel);
    doc.addToIndexes();
    
    // Adding populated FeatureStructure to CAS
    jcas.addFsToIndexes(doc);
  }

  // Parse a single data line to get query ID, relevant value and text string.
  private static ArrayList<String> parseDataLine(String line) {
    ArrayList<String> docInfo;

    String[] rec = line.split("[\\t]");
    String sResQid = (rec[0]).replace("qid=", "");
    String sResRel = (rec[1]).replace("rel=", "");

    StringBuffer sResTxt = new StringBuffer();
    for (int i = 2; i < rec.length; i++) {
      sResTxt.append(rec[i]).append(" ");
    }

    docInfo = new ArrayList<String>();
    docInfo.add(sResRel);
    docInfo.add(sResQid);
    docInfo.add(sResTxt.toString());
    return docInfo;
  }

}