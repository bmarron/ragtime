package llm.devoxx.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import llm.devoxx.json.CompleteAnswer;
import llm.devoxx.json.Question;
import llm.devoxx.services.QuestionService;

@Path(("/question"))
public class QuestionResource {

    @Inject
    QuestionService questionService;


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public CompleteAnswer question(Question question) {
        return questionService.processQuestion(question);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("memory")
    public CompleteAnswer questionMemory(Question question) {
        return questionService.chatWithDocsAndMemory(question);
    }

}
