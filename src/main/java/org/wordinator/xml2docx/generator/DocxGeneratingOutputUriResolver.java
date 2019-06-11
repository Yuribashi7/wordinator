package org.wordinator.xml2docx.generator;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlSaxHandler;

import net.sf.saxon.lib.OutputURIResolver;

/**
 * Saxon S9 OutputURIResolver implementation that takes the result and generates
 * a DOCX file from it.
 *
 */
public class DocxGeneratingOutputUriResolver implements OutputURIResolver {
	
	public static Logger log = LogManager.getLogger();

	private File outDir;
	private XmlSaxHandler saxHandler;

	private int dotsPerInch = 96; // FIXME: Need to figure out a way to make this
	                              // configurable given that resolver is created using
								  // newInstance()

	private XWPFDocument templateDoc;

	/**
	 * 
	 * @param outDir Directory to put new DOCX files into.
	 * @param templateDoc The DOTX template to use in constructing new DOCX files.
	 * @param log 
	 */
	public DocxGeneratingOutputUriResolver(File outDir, XWPFDocument templateDoc, Logger log) {
		this.outDir = outDir;
		this.templateDoc = templateDoc;		
		DocxGeneratingOutputUriResolver.log = log;
		
	}

	public OutputURIResolver newInstance() {
		return new DocxGeneratingOutputUriResolver(outDir, templateDoc, log);
	}

	public Result resolve(String href, String base) throws TransformerException {
		saxHandler = XmlObject.Factory.newXmlSaxHandler();
	
		Result result = new SAXResult(saxHandler.getContentHandler());
		result.setSystemId(href);
		return result;
		
	}

	public void close(Result result) throws TransformerException {
		// Do the DOCX building
		
		try {
			XmlObject xml = saxHandler.getObject();
			String outFilepath = URLDecoder.decode(result.getSystemId(), "UTF-8");
			String filename = FilenameUtils.getBaseName(outFilepath) + ".docx";
			
//			/*
//			 * If file pattern is for a 'section' then output to ~\DOCX\SECCHUNK\...
//			 */
//			String patternString = "\\d{5}_\\d{1,5}.docx";
//			Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
//			Matcher matcher = pattern.matcher(filename);
//			boolean isMatched = matcher.matches();
//			
//			if(isMatched) {
//			    String origPath = outDir.getCanonicalPath();
//			    String destPath = origPath.replace(outDir.getName(),"");
//System.out.println("\n+[debug destPath]: " + destPath);  
//			    String destFile = outDir.getName().toString() + "\\SECCHUNK";
//System.out.println("\n+[debug destFile]: " + destFile);  
//			    
//			    File newOutDir = new File(destPath + destFile);
//			    
//System.out.println("\n+[debug newoutDir: ] " + newOutDir.toString() + "\n");
			    
//			    File outFile = new File(newOutDir, filename);
//				File inFile = new File(new URL(result.getSystemId()).toURI());
//				log.info("Generating DOCX file \"" + outFile.getAbsolutePath() + "\"");
//				DocxGenerator generator = new DocxGenerator(inFile, outFile, templateDoc);
//				generator.setDotsPerInch(dotsPerInch);
//				generator.generate(xml);
//				
//			} else {
//			
//				File outFile = new File(outDir, filename);
//				File inFile = new File(new URL(result.getSystemId()).toURI());
//				log.info("Generating DOCX file \"" + outFile.getAbsolutePath() + "\"");
//				DocxGenerator generator = new DocxGenerator(inFile, outFile, templateDoc);
//				generator.setDotsPerInch(dotsPerInch);
//				generator.generate(xml);
//			}

			File outFile = new File(outDir, filename);
			File inFile = new File(new URL(result.getSystemId()).toURI());
			log.info("Generating DOCX file \"" + outFile.getAbsolutePath() + "\"");
			DocxGenerator generator = new DocxGenerator(inFile, outFile, templateDoc);
			generator.setDotsPerInch(dotsPerInch);
			generator.generate(xml);
		} catch (Exception e) {
			throw new TransformerException(e);
		}

	}

	public int getDotsPerInch() {
		return dotsPerInch;
	}

}















