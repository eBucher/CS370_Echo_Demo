package com.wolfpack.servlet;

import com.neong.voice.speechlet.TemplateBaseSkillSpeechlet;

import com.amazon.speech.speechlet.servlet.SpeechletServlet;


public class WolfpackSpeechletServlet extends SpeechletServlet {
	public WolfpackSpeechletServlet() {
		super();
		this.setSpeechlet(new TemplateBaseSkillSpeechlet());
	}

	/*
	 * "...it is _strongly recommended_ that all serializable classes
	 * explicitly declare serialVersionUID values..."
	 *
	 * http://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html
	 */
	private static final long serialVersionUID = 1L;
}
