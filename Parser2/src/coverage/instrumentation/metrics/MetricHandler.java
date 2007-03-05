package coverage.instrumentation.metrics;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;

import coverage.instrumentation.bpelxmltools.BpelXMLTools;
import coverage.instrumentation.metrics.branchcoverage.BranchMetric;
import coverage.instrumentation.metrics.statementcoverage.Statementmetric;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;
import de.schlichtherle.io.FileWriter;
import exception.BpelException;
import exception.BpelVersionException;

/**
 * Die Klasse implementiert das Interface IMetricHandler.
 * 
 * @author Alex Salnikow
 */
public class MetricHandler implements IMetricHandler {

	private static final Namespace NAMESPACE_BPEL_2 = Namespace
			.getNamespace("http://schemas.xmlsoap.org/ws/2003/03/business-process/");

	private static IMetricHandler instance = null;

	private Hashtable metrics;

	private Element process_element;

	private Logger logger;

	public static IMetricHandler getInstance() {
		if (instance == null) {
			instance = new MetricHandler();
		}
		System.out.println("!!!!!!!!!!!!!!!!HAT GEKLAPPT!!!!!!!!!!!!!");
		return instance;
	}

	private MetricHandler() {
		metrics = new Hashtable();
		logger = Logger.getLogger(getClass());
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		logger.addAppender(consoleAppender);
		FileAppender fileAppender;
		try {
			fileAppender = new FileAppender(layout, "MeineLogDatei.log", false);
			logger.addAppender(fileAppender);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public IMetric addMetric(String metricName) {
		IMetric metric = null;
		if (metricName.equals(STATEMENT_METRIC)) {
			metric = Statementmetric.getInstance();
			metrics.put(STATEMENT_METRIC, metric);
		} else if (metricName.equals(BRANCH_METRIC)) {
			metric = BranchMetric.getInstance();
			metrics.put(BRANCH_METRIC, metric);
		}
		return metric;
	}

	public void remove(String metricName) {
		if (metricName.equals(STATEMENT_METRIC)) {
			metrics.remove(Statementmetric.getInstance());
		} else if (metricName.equals(BRANCH_METRIC)) {
			metrics.remove(Statementmetric.getInstance());
		}

	}

	public void startInstrumentation(File file) throws JDOMException,
			IOException, BpelException, BpelVersionException {

		logger.info("Die Instrumentierung der Datei " + file.getName()
				+ " wird gestartet.");
		SAXBuilder builder = new SAXBuilder();
		FileInputStream is=new FileInputStream(file);
		Document doc = builder.build(is);
		process_element = doc.getRootElement();
		if (!process_element.getName().equalsIgnoreCase(
				BpelXMLTools.PROCESS_ELEMENT)) {
			throw (new BpelException(BpelException.NO_VALIDE_BPEL));
		}
		System.out.println(process_element.getNamespace());
		if (!process_element.getNamespace().equals(NAMESPACE_BPEL_2)) {
			throw (new BpelVersionException(BpelVersionException.WRONG_VERSION));
		}
		BpelXMLTools.process_element = process_element;

		// xmlns:ns2="http://www.bpelunit.org/coverage/logService"
		// <bpel:import importType="http://schemas.xmlsoap.org/wsdl/"
		// location="../wsdl/_LogService_.wsdl"
		// namespace="http://www.bpelunit.org/coverage/logService"/>
		// <bpel:partnerLink name="PLT_LogService_"
		// partnerLinkType="ns2:PLT_LogService_" partnerRole="Logger"/>
		// <bpel:variable messageType="ns2:logRequest" name="logRequest"/>

		process_element.addNamespaceDeclaration(Namespace.getNamespace("log",
				"http://www.bpelunit.org/coverage/logService"));
		Element importElement = new Element("import", BpelXMLTools
				.getBpelNamespace());
		importElement.setAttribute("importType",
				"http://schemas.xmlsoap.org/wsdl/");
		importElement.setAttribute("location", "../wsdl/_LogService_.wsdl");
		importElement.setAttribute("namespace",
				"http://www.bpelunit.org/coverage/logService");
		process_element.addContent(0, importElement);
		//		
		Element partnerLinks = process_element.getChild("partnerLinks",
				BpelXMLTools.getBpelNamespace());
		Element partnerLink = new Element("partnerLink", BpelXMLTools
				.getBpelNamespace());
		partnerLink.setAttribute("name", "PLT_LogService_");
		partnerLink.setAttribute("partnerLinkType", "log:PLT_LogService_");
		partnerLink.setAttribute("partnerRole", "Logger");
		partnerLinks.addContent(partnerLink);
		int index = process_element.indexOf(partnerLinks);

		Element variable = new Element("variable", BpelXMLTools
				.getBpelNamespace());
		variable.setAttribute("messageType", "log:logRequest");
		variable.setAttribute("name", "logRequest");
		Element variables = process_element.getChild("variables", BpelXMLTools
				.getBpelNamespace());
		if (variables == null) {
			variables = new Element("variables", BpelXMLTools
					.getBpelNamespace());
			process_element.addContent(index + 1, variables);
		}
		variables.addContent(variable);

		IMetric metric;
		for (Enumeration<IMetric> i = metrics.elements(); i.hasMoreElements();) {
			metric = i.nextElement();
			logger.info(metric);
			metric.insertMarker(process_element);
		}
		// String[] name_analyse = getFileName(file);
		// File instrumentation_file=new
		// File(name_analyse[0]+"_instr_"+name_analyse[1]);
		 File instrumentation_file = new File("ergebnis.bpel");
		logger.info("Instrumentierung erfolgreich ausgeführt.");
		// logger.info("Die instrumentierte BPEL-Datei wird in
		// "+instrumentation_file+" geschrieben.");

		FileWriter writer = new FileWriter(instrumentation_file);
		XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
		xmlOutputter.output(doc, writer);
		is.close();
		writer = new FileWriter(file);
		xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
		xmlOutputter.output(doc, writer);
		writer.close();
	}

	public IMetric getMetric(String metricName) {
		IMetric metric = null;
		if (metricName.equals(STATEMENT_METRIC)) {
			metric = Statementmetric.getInstance();
		} else if (metricName.equals(BRANCH_METRIC)) {
			metric = BranchMetric.getInstance();
		}
		return metric;
	}

}
