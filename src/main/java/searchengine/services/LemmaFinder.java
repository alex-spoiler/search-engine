package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class LemmaFinder {
    private static final Pattern PATTERN_PARTICLE = Pattern.compile("(МЕЖД|ПРЕДЛ|СОЮЗ|ЧАСТ)");
    private final LuceneMorphology luceneMorph;

    public static LemmaFinder getInstance() {
        LuceneMorphology morphology= null;
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new LemmaFinder(morphology);
    }

    private LemmaFinder(LuceneMorphology luceneMorph) {
        this.luceneMorph = luceneMorph;
    }

    private LemmaFinder(){
        throw new RuntimeException("Disallow construct");
    }

    public HashMap<String, Integer> getLemmasCollection(String text) {
        String[] words = getArrayRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank() || isServicePartOfSpeech(word)) {
                continue;
            }

            List<String> normalForms = luceneMorph.getNormalForms(word);

            if (!normalForms.isEmpty()) {
                String normalWord = normalForms.get(0);
                lemmas.put(normalWord, lemmas.containsKey(normalWord) ? lemmas.get(normalWord) + 1 : 1);
            }
        }
        return lemmas;
    }

    private String[] getArrayRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim().split("\\s+");
    }

    private boolean isServicePartOfSpeech(String word) {
        List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
        for (String s : wordBaseForms) {
            if (PATTERN_PARTICLE.matcher(s).find()) {
                return true;
            }
        }
        return false;
    }
}