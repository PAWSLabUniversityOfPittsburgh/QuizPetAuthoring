package edu.pitt.pawslab.quizpet.instance;

import java.util.regex.Pattern;

public class Setting {
	
	public final static String DOMAIN = "py";
	public final static String QPYPREFIX = "q_py";
	public final static String DBNAME = "webex21";
	public final static String TIMEFORMAT = "MM/dd/yyyy HH:mm:ss";
	
	public final static Pattern PYTHONPREFFIX = Pattern.compile("^q_py_(?=.*)");
	public final static Pattern VERSIONSUFFIX = Pattern.compile("(?=.*)_v(\\d+)$");
	
	public final static String PYTHONCLASSSUFFIX = "py";
	public final static String PYTHONCLASSFOLDER = System.getProperty("user.home");
	
	public final static String PARSERURL = "http://acos.cs.hut.fi/python-parser";
	public final static String PARSERCODENAME = "code";
	public final static String PARSERTYPENAME = "mode";
	public final static String PARSERTYPEVALUE = "hierarchical";

}
