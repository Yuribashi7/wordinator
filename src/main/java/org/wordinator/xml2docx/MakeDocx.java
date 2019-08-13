package org.wordinator.xml2docx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlObject;
import org.wordinator.xml2docx.generator.DocxGeneratingOutputUriResolver;
import org.wordinator.xml2docx.generator.DocxGenerator;
import org.wordinator.xml2docx.saxon.Log4jSaxonLogger;
import org.wordinator.xml2docx.saxon.LoggingMessageListener;

import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;

/**
 * Command-line application to generate DOCX files from
 * 	
 * <p>You can use this directly as the main file run from the command line
 * or as a helper class to build your own command-line handler or integrated
 * DOCX generator.
 *
 */
public class MakeDocx 
{	
	public static final Logger log = LogManager.getLogger(MakeDocx.class.getSimpleName());			
	public static final String XSLT_PARAM_CHUNKLEVEL = "chunklevel";

	public static void main( String[] args ) throws ParseException
    {
    	Options options = buildOptions();
     	handleCommandLine(options, args, log); 	  	
    }

	/**
	 * Does the actual command line processing. You can call this from your own
	 * command line processor if you need additional command-line options, for example,
	 * to set additional XSLT parameters.
	 * @param options Command-line options
	 * @param args Command-line arguments
	 * @param log Logger to log messages to.
	 * @throws ParseException Thrown if there is problem parsing the input
	 */
	public static void handleCommandLine(
			Options options,
			String[] args,
			Logger log) throws ParseException {
    	CommandLineParser parser = new DefaultParser();
    	CommandLine cmd = parser.parse( options, args);
    	
    	Map<String, String> xsltParameters = new HashMap<String, String>();
    	String myCatalogs = cmd.getOptionValue("k");
		String inDocPath = cmd.getOptionValue("i");
    	String docxPath = cmd.getOptionValue("o");
    	String templatePath = cmd.getOptionValue("t");
    	String transformPath = cmd.getOptionValue("x");
    	String chunkLevel = cmd.getOptionValue("c");
    	
    	chunkLevel = chunkLevel == null ? "root" : chunkLevel;
    	
    	log.info("myCatalogs                 ='" + myCatalogs + "'");
    	log.info("Input document or directory='" + inDocPath + "'");
    	log.info("Output directory           ='" + docxPath + "'");
    	log.info("DOTX template              ='" + templatePath + "'");
    	log.info("XSLT template              =" + (transformPath == null ? "Not specified" : "'" + transformPath + "'"));
    	log.info("Chunk level                ='" + chunkLevel + "'");
    	
    	// Check that the input file exists.
    	// For now, always overwriting the DOCX file without confirmation.
    	
    	File inFile = new File(inDocPath);
    	
    	if (!inFile.exists()) {
    		log.error("Input file '" + inFile.getAbsolutePath() + "' not found. Cannot continue."); 
    		System.exit(1);
    	} else {
    		log.info("FOUND inFile: " + inFile.getName());
    	}
    	
    	File templateFile = new File(templatePath);
    	if (!templateFile.exists()) {
    		log.error("Template file '" + templateFile.getAbsolutePath() + "' not found. Cannot continue."); 
    		System.exit(1);
    	}
    	
		XWPFDocument templateDoc = null;
		try {
			templateDoc = new XWPFDocument(new FileInputStream(templateFile));
		} catch (Exception e) {
			log.error(e.getClass().getSimpleName() +  " loading template DOCX file \"" + templateFile.getAbsolutePath() + "\"");
			System.exit(1);
		}
        
    	File outFile = new File(docxPath);
    	
    	File outDir = outFile; // Normal case: specify output directory
    	if (outFile.getName().endsWith(".docx")) {
    		outDir = outFile.getParentFile();
    	}
    	
    	if (!outDir.exists()) {
    		log.info("Making output directory '" + outDir.getAbsolutePath() + "'...");
    		if (!outDir.mkdirs()) {
    			log.error("Failed to create output directory '" + outDir.getAbsolutePath() + "'. Cannot continue");
        		System.exit(1);
    		}
    	}
    	
    	File transformFile = null;
    	if (null != transformPath) {
    		transformFile = new File(transformPath);
        	if (!transformFile.exists()) {
        		log.error("XSLT transform file '" + transformFile.getAbsolutePath() + "' not found. Cannot continue."); 
        		System.exit(1);
        	}
        	if (!xsltParameters.containsKey(XSLT_PARAM_CHUNKLEVEL)) {
        		xsltParameters.put(XSLT_PARAM_CHUNKLEVEL, chunkLevel);
        	}
    	}
    	
    	try {
    		if (inFile.isDirectory()) {
    			// Assume directory contains *.swpx files 
System.out.println("\n...handleDirectory...\n");   			    			
    			handleDirectory(inFile, outDir, templateDoc, log);
    		} else { 
    			if (inFile.getName().endsWith(".swpx")) {

System.out.println("\n...handleSingleSwpxDoc...\n");   			
	    			handleSingleSwpxDoc(inFile, outFile, templateDoc, log);
	    		} else {

System.out.println("\n...transformXml...\n");   	
	    			transformXml(inFile, outDir, templateDoc, transformFile, xsltParameters, log);
	    		}
    		}
    	} catch (Exception e) {
    		log.error(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
    		System.exit(1);
    	} finally {
			try {
				templateDoc.close();
			} catch (IOException e) {
				// Don't care about this should it ever happen.
			}
		}
    	
	}

	/**
	 * Process an XML document to a set of DOCX files
	 * @param docFile the root XML document to process
	 * @param outDir Directory to put the DOCX files in
	 * @param templateDoc Template DOCX document
	 * @param transformFile The file containing the XSLT transform for generating SWPX documents
	 * @param xsltParameters Map of parameter names to values to be passed to the XSLT transform.
	 * @param log Log to write messages to.
	 * @throws Exception Any kind of error
	 */
	
	/*
	 * SAXON NOTES (Michael Kay):
	 * First the stack trace shows that you are invoking the processor with net.sf.saxon.query.StaticQueryContext.compileQuery 
	 * which is a pretty low-level entry point, and there are many ways of getting things wrong at this level. 
	 * I would recommend you use the s9api interface (XQueryCompiler.compile()).
	 * 
	 * One of the things you can easily get wrong is to run without Saxon-PE/EE enabled. Calls to reflexive 
	 * extension functions require at least Saxon-PE, which also needs a license file to be present. 
	 * Check that the Saxon Configuration is a ProfessionalConfiguration or an EnterpriseConfiguration.
	 * 
	 * Using the -TJ option on the command line, or the equivalent configuration property in the 
	 * API (FeatureKeys.TRACE_EXTERNAL_FUNCTIONS) will give you better diagnostics as to why it is failing to find the function.
	 * 
	 * Finally, note that using -jar on the Java command line means that the class path is ignored: everything 
	 * has to be loaded from the JAR file itself. This makes it quite difficult to pick up the license file and 
	 * any external library classes, so it is best avoided.
	 */
	public static void transformXml(
			File docFile, 
			File outDir, 
			XWPFDocument templateDoc, 
			File transformFile, 
			Map<String, String> xsltParameters, 
			Logger log) throws Exception {
		// Apply transform to book file to generate Simple WP XML documents
		
		if (transformFile == null) {
			throw new RuntimeException("-x (transform) parameter not specified. If the input is a _Book.xml file, you must specify the -x parameter");
		}
		
		StandardErrorListener errorListener = new StandardErrorListener();
		net.sf.saxon.lib.Logger saxonLogger = new Log4jSaxonLogger(log);
		errorListener.setLogger(saxonLogger);		

		// Example: (https://www.saxonica.com/documentation/#!using-xsl/embedding/s9api-transformation)
/*
	   Processor processor = new Processor(false);
	   XsltCompiler compiler = processor.newXsltCompiler();
	   XsltExecutable stylesheet = compiler.compile(new StreamSource(new File("styles/books.xsl")));
	   Serializer out = processor.newSerializer(new File("books.html"));
	   out.setOutputProperty(Serializer.Property.METHOD, "html");
	   out.setOutputProperty(Serializer.Property.INDENT, "yes");
	   Xslt30Transformer transformer = stylesheet.load30();
	   transformer.transform(new StreamSource(new File("data/books.xml")), out);
*/			
		
		Processor processor = new Processor(true);
		System.out.println("\n...SaxonEdition: " + processor.getSaxonEdition() + "\n");
		processor.setConfigurationProperty(FeatureKeys.LICENSE_FILE_LOCATION, "C:\\SaxonPE9-9-1-4J\\saxon-license.lic");
		
		processor.setConfigurationProperty(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS, true);
		
		DocxGeneratingOutputUriResolver outputResolver = new DocxGeneratingOutputUriResolver(outDir, templateDoc, log);
		System.out.println("...outputResolver: " + outputResolver.toString() + "\n");
		
		
		processor.setConfigurationProperty(FeatureKeys.OUTPUT_URI_RESOLVER, outputResolver);

		
		// FIXME: Set up proper logger. See 
		// https://www.saxonica.com/html/documentation/using-xsl/embedding/s9api-transformation.html
		// https://www.saxonica.com/documentation/#!using-xsl/embedding/s9api-transformation
		XsltCompiler compiler = processor.newXsltCompiler();
		
		InputStream inStream = new FileInputStream(transformFile);
		Source xformSource = new StreamSource(inStream); 
		xformSource.setSystemId(transformFile.toURI().toURL().toExternalForm());
		XsltExecutable executable = compiler.compile(xformSource);
		
		Xslt30Transformer transformer = executable.load30();
		transformer.setErrorListener(errorListener);
		
		MessageListener messageListener = new LoggingMessageListener(log);
		transformer.setMessageListener(messageListener);

		Map<QName, XdmValue> parameters = new HashMap<QName, XdmValue>();
		// Assuming that parameters are not namespaced. If they are we'll
		// have to deal with that additional complexity. s
		for (String name : xsltParameters.keySet()) {
			parameters.put(new QName("", name), XdmValue.makeValue(xsltParameters.get(name)));			
		}
		transformer.setStylesheetParameters(parameters);
		
		Source docSource = new StreamSource(docFile);
		log.info("Applying transform to source document " + docFile.getAbsolutePath() + "...");
		
		@SuppressWarnings("unused")
		XdmValue result = transformer.applyTemplates(docSource);
		log.info("Transform applied.");
	}

	/**
	 * Process a SWPX file to a DOCX file.
	 * @param inFile Single SWPX file
	 * @param outFile If this is a directory, result filename is constructed from input filename. 
	 * @param templateDoc Template DOCX document used when constructing new document
	 * @param log Log to put messages to.
	 */
	public static void handleSingleSwpxDoc(File inFile, File outFile, XWPFDocument templateDoc, Logger log) {
		
		File effectiveOutFile = outFile;
		if (outFile.isDirectory()) {
			String outName = FilenameUtils.getBaseName(inFile.getAbsolutePath()) + ".docx";
			effectiveOutFile = new File(outFile, outName);
		}

    	try {
    		log.info("Generating DOCX file \"" + effectiveOutFile.getAbsolutePath() + "\"");
			if (effectiveOutFile.exists()) {
				if (!effectiveOutFile.delete()) {
					log.error("Could not delete existing DOCX file \"" + effectiveOutFile.getAbsolutePath() + "\". Skipping SWPX file.");
					return;
				}
			}
	    	DocxGenerator generator = new DocxGenerator(inFile, effectiveOutFile, templateDoc);
			XmlObject xml = XmlObject.Factory.parse(inFile);

			generator.generate(xml);
			log.info("DOCX file generated.");
		} catch (Throwable e) {
			log.error("Unexpected " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}		
	}

	/**
	 * Process all *.swpx files in the input directory, putting the results in the output directory.
	 * <p>NOTE: This method is primarily for testing purposes. During production the SWPX docs are
	 * generated dynamically from the _Book.xml file.</p> 
	 * @param inDir Directory to look for *.swpx files in
	 * @param outDir Directory to write *.docx files to
	 * @param templateDoc Template DOCX document used when constructing new document
	 * @param log Log to write messages to.
	 */
	public static void handleDirectory(File inDir, File outDir, XWPFDocument templateDoc, Logger log) {
		
		FilenameFilter filter = new SuffixFileFilter(".swpx");
		File[] files = inDir.listFiles(filter);
		for (File inFile : files) {
			handleSingleSwpxDoc(inFile, outDir, templateDoc, log);
		}

	}

	/**
	 * Build the command-line options
	 * @return CLI options object ready to use.
	 */
	public static Options buildOptions() {
		Options options = new Options();
    	Option input = Option.builder("i")
						.required(true)
						.hasArg(true)
						.desc("The path and filename of the Simple WP XML document or directory containing .swpx files.")
						.build();
    	Option output = Option.builder("o")
						.required(true)
						.hasArg(true)
						.desc("The path and filename of the result DOCX file, or directory to contain generated DOCX files")
						.build();
    	Option template = Option.builder("t")
				.required(true)
				.hasArg(true)
				.desc("The path and filename of the template DOTX file.")
				.build();
    	Option transform = Option.builder("x")
				.required(false)
				.hasArg(true)
				.desc("The path and filename of the XSLT transform for generating SWPX documents.")
				.build();
    	Option dpi = Option.builder("d")
    			.longOpt("dpi")
				.required(false)
				.hasArg(true)
				.desc("The dots-per-inch value to use when converting pixels to absolute measurements, e.g., \"72\" or \"96\".")
				.build();
    			
    	options.addOption(input);
    	options.addOption(output);
    	options.addOption(template);
    	options.addOption(transform);
    	options.addOption(dpi);

		return options;
	}
}
