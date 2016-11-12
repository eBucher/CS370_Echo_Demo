package com.neong.voice.wolfpack;

import java.util.Map;

public class Synonym {
	/**
	 * @param phrase     A string that will be matched to one of the correct synonyms.
	 * @param synonyms   A dictionary of (term: definition) pairs.
	 * @return           The definition of the term most similar to phrase.
	 */
	public static String getSynonym(final String phrase, final Map<String, String> synonyms) {
		final String bestKey = CosineSim.getBestMatch(phrase, synonyms.keySet());
		return synonyms.get(bestKey);
	}
}
