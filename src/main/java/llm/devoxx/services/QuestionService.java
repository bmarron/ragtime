package llm.devoxx.services;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import llm.devoxx.json.Answer;
import llm.devoxx.json.CompleteAnswer;
import llm.devoxx.json.Question;
import llm.devoxx.util.Constants;
import llm.devoxx.util.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class QuestionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionService.class);
    @Inject
    Tools tools;

    @Inject
    EmbeddingModel embeddingModel;

    public CompleteAnswer processQuestion(Question question) {

        EmbeddingStore<TextSegment> store = tools.getStore();
        //EmbeddingModel model = tools.createEmbeddingModel();
        LanguageModel languageModel = tools.createLanguageModel();

        Embedding queryEmbedded = embeddingModel.embed(question.getQuestion()).content();

        List<EmbeddingMatch<TextSegment>> relevant = store.findRelevant(queryEmbedded,3, 0.55);

        List<Answer> answers = new ArrayList<>();

        StringBuilder rep = new StringBuilder(question.getQuestion());
        LOGGER.info("Generating answer for question: \"{}\"", question.getQuestion());
        for (var rel : relevant) {
            answers.add(new Answer(rel.embedded().text(), rel.score(), rel.embedded().metadata()));

            rep.append(System.lineSeparator());
            rep.append(rel.embedded().text());
        }

        if (question.isGenerateAnswer()) {
            Response<String> generatedAnswer = languageModel.generate(rep.toString());
            return new CompleteAnswer(generatedAnswer.content(), answers);
        }

        return new CompleteAnswer(Constants.EMPTY_STRING, answers);

    }

}
