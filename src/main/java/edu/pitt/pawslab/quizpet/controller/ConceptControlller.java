package edu.pitt.pawslab.quizpet.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.pitt.pawslab.quizpet.instance.ConceptNode;
import edu.pitt.pawslab.quizpet.instance.Quiz;
import edu.pitt.pawslab.quizpet.instance.ServerMessage;
import edu.pitt.pawslab.quizpet.service.ConceptService;
import edu.pitt.pawslab.quizpet.service.QuizService;

@Controller
@RequestMapping(value = "/concept")
public class ConceptControlller {
	
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private QuizService quizService;
	
	private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
	private static Locale locale = new Locale("en");

	@RequestMapping(value = "/get/{quizId}", method = RequestMethod.GET)
	public @ResponseBody HashMap<String, Object[]> getConceptPackgeForOneQuiz(@PathVariable Integer quizId){
		logger.info("getting concepts of quiz id " + quizId, locale);
		HashMap<String, Object[]> rs = new HashMap<String, Object[]>();
		ServerMessage quiz = quizService.getQuizById(quizId);
		if(quiz.isSuccess()){
			Quiz currQuiz = (Quiz) quiz.getContent()[0];
			return conceptService.getConceptsAndCodeOfOneQuiz(currQuiz);
		}else{
			return rs;
		}
	}
	
	@RequestMapping(value = "/update/{quizId}", method = RequestMethod.PUT)
	public @ResponseBody ServerMessage updateConceptForOneQuiz(@PathVariable Integer quizId, @RequestBody ArrayList<ConceptNode> newConcepts){
		ServerMessage serverMessage = new ServerMessage();
		logger.info("updating concepts of quiz id " + quizId, locale);
		ServerMessage quiz = quizService.getQuizById(quizId);
		if(quiz.isSuccess()){
			Quiz currQuiz = (Quiz) quiz.getContent()[0];
			Integer rows = conceptService.updateConceptsOfOneQuiz(currQuiz, newConcepts);
			serverMessage.setSuccess(true);
			serverMessage.setMessage(new StringBuilder("Concepts updated."));
		}else{
			serverMessage.setSuccess(false);
			serverMessage.setMessage(new StringBuilder("No such quiz."));
		}
		
		return serverMessage;
	}
}
