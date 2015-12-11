package edu.pitt.pawslab.quizpet.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pitt.pawslab.quizpet.database.WebexDatabase;
import edu.pitt.pawslab.quizpet.instance.ConceptNode;
import edu.pitt.pawslab.quizpet.instance.Quiz;
import edu.pitt.pawslab.quizpet.instance.ServerMessage;
import edu.pitt.pawslab.quizpet.instance.Setting;

@Service
public class ConceptService {
	
	@Autowired
	WebexDatabase webexDatabase;
	@Autowired
	QuizService quizService;
	@Autowired
	ExternalClassService externalClassService;
	
	private final static ObjectMapper objectMapper = new ObjectMapper();
	
	private static StringBuilder parseCode(StringBuilder code) throws Exception{
		URL url = new URL(Setting.PARSERURL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		
		String parameters = Setting.PARSERTYPENAME + "=" + Setting.PARSERTYPEVALUE + "&"
				+ Setting.PARSERCODENAME + "=" + UriUtils.encodeQueryParam(code.toString(), "UTF-8");
		
		connection.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(parameters);
		wr.flush();
		wr.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while((inputLine = in.readLine()) != null){
			response.append(inputLine);
		}
		in.close();
		
		return response;
	}
	
	private static ArrayList<ConceptNode> parseConceptTree(StringBuilder codeTree){
		try{
			ConceptNode root = objectMapper.readValue(codeTree.toString(), ConceptNode.class);
			ArrayList<ConceptNode> conceptList = new ArrayList<ConceptNode>();
			ConceptNode.traverse(conceptList, root);
			return conceptList;
		}catch(Exception e){
			return null;
		}
	}

	public ServerMessage injectConceptForOneQuiz(Integer quizId) throws Exception{
		ServerMessage serverMessage = new ServerMessage();
		HashMap<StringBuilder, StringBuilder> codeParts = quizService.getCodePartsByQuizId(quizId);
		
		HashMap<StringBuilder, ArrayList<ConceptNode>> conceptListForOneQuiz = new HashMap<StringBuilder, ArrayList<ConceptNode>>();
		Iterator<StringBuilder> iterator = codeParts.keySet().iterator();
		while(iterator.hasNext()){
			StringBuilder filename = iterator.next();
			StringBuilder code = codeParts.get(filename);
			ArrayList<ConceptNode> nodelist = parseConceptTree(parseCode(code));
			if(nodelist != null){
				conceptListForOneQuiz.put(filename, nodelist);
			}else{
				serverMessage.setSuccess(false);
				serverMessage.setMessage(new StringBuilder(filename + " has errors in its code. Concept injection failed."));
			}
		}
		
		Quiz quiz = webexDatabase.getQuizById(quizId);
		String quizRdfId = quiz.getRdfIdInDb();
		if(webexDatabase.checkConcepts(quizRdfId) > 0){
			webexDatabase.removeConcepts(quizRdfId);
		}
		if(webexDatabase.addConceptsByClassFile(quizRdfId, conceptListForOneQuiz) > 0){
			serverMessage.setSuccess(true);
			serverMessage.setContent(new Object[]{quizRdfId, conceptListForOneQuiz});
		}else{
			serverMessage.setSuccess(false);
			serverMessage.setMessage(new StringBuilder("Concept injection failed."));
		}

		return serverMessage;
	}
	
	public HashMap<String, ArrayList<ConceptNode>> getConceptsOfOneQuiz(String rdfIdInDb){
		ArrayList<ConceptNode> list = webexDatabase.getAllConceptsOfOneQuiz(rdfIdInDb);
		HashMap<String, ArrayList<ConceptNode>> rs = new HashMap<String, ArrayList<ConceptNode>>();
		
		Iterator<ConceptNode> iterator = list.iterator();
		while(iterator.hasNext()){
			ConceptNode curr = iterator.next();
			String classFile = curr.getClassFile().toString();
			if(rs.containsKey(classFile)){
				ArrayList<ConceptNode> currList = rs.get(classFile);
				currList.add(curr);
			}else{
				ArrayList<ConceptNode> currList = new ArrayList<ConceptNode>();
				currList.add(curr);
				rs.put(classFile, currList);
			}
		}
		
		return rs;
	}
	
	public HashMap<String, Object[]> getConceptsAndCodeOfOneQuiz(Quiz quiz){
		HashMap<String, Object[]> rs = new HashMap<String, Object[]>();
		HashMap<String, ArrayList<ConceptNode>> concepts = getConceptsOfOneQuiz(quiz.getRdfIdInDb());
		
		Iterator<String> iterator = concepts.keySet().iterator();
		while(iterator.hasNext()){
			String classFileName = iterator.next();
			ArrayList<ConceptNode> conceptsForThisClass = concepts.get(classFileName);
			
			if(classFileName.equals("Tester.py")){
				//code from this quiz
				StringBuilder code = quiz.getCode();
				Object[] value = new Object[]{code, conceptsForThisClass};
				rs.put(classFileName, value);
			}else{
				//code from external classes
				Integer externalClassId = webexDatabase.getClassIdByFileName(classFileName);
				ServerMessage classCode = externalClassService.getClassById(externalClassId);
				if(classCode.isSuccess()){
					StringBuilder code = (StringBuilder) classCode.getContent()[0];
					Object[] value = new Object[]{code, conceptsForThisClass};
					rs.put(classFileName, value);
				}
			}
		}
		
		return rs;
	}
	
	public Integer updateConceptsOfOneQuiz(Quiz quiz, ArrayList<ConceptNode> newConcepts){
		String rdfIdInDb = quiz.getRdfIdInDb();
		webexDatabase.removeConcepts(rdfIdInDb);
		
		return webexDatabase.addConcepts(rdfIdInDb, newConcepts);
	}
}
