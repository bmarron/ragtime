package llm.devoxx.services;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionService.class);

    @Inject
    Tools tools;

    @Inject
    EmbeddingModel embeddingModel;

    public CompleteAnswer processQuestion(Question question) {

        EmbeddingStore<TextSegment> store = tools.getStore();

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

            PromptTemplate template = PromptTemplate.from(
                    "Answer the following question :\n"
                        + "\n"
                        + "Question:\n"
                        + "{{question}}\n"
                        + "\n"
                        + "Base your answer on the following information:\n"
                        + "{{information}}"
            );

            String information = relevant.stream().map(rlv -> rlv.embedded().text()).collect(Collectors.joining("\n\n"));
            Map<String, Object> templateParameters = new HashMap<>();
            templateParameters.put("question", question.getQuestion());
            templateParameters.put("information", information);
            Prompt prompt = template.apply(templateParameters);

            Response<String> generatedAnswer = languageModel.generate(prompt);
            return new CompleteAnswer(generatedAnswer.content(), answers);
        }

        return new CompleteAnswer(Constants.EMPTY_STRING, answers);

    }

}
