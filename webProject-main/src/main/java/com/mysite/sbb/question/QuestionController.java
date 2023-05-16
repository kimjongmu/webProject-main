package com.mysite.sbb.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.validation.BindingResult;
import com.mysite.sbb.answer.AnswerForm;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.IOException;

import java.security.Principal;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



@RequestMapping("question")
@RequiredArgsConstructor
@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final UserService userService;

    @RequestMapping("list")
    public String list(Model model, @RequestParam(value="page", defaultValue="0") int page,
    @RequestParam(value = "kw", defaultValue = "") String kw) throws IOException, ParserConfigurationException, SAXException {
        Page<Question> paging = this.questionService.getList(page,kw);
        List<EmergencyRoom> erList = new ArrayList<>();

        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);

        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmRltmUsefulSckbdInfoInqire"); /*URL*/
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=nudyCur0o0vscHCyQTyQlSfRcBURzPdahs5Ced5hgepCzwwRHm5OCe181NQzy%2B6pYr4vZcNSYVHpn6f8fO9YAw%3D%3D"); /*Service Key*/
        urlBuilder.append("&" + URLEncoder.encode("STAGE1","UTF-8") + "=" + URLEncoder.encode("대전", "UTF-8")); /*주소(시도)*/
        urlBuilder.append("&" + URLEncoder.encode("STAGE2","UTF-8") + "=" + URLEncoder.encode("중구", "UTF-8")); /*주소(시군구)*/
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String parsingUrl="";
        parsingUrl=url.toString();

        DocumentBuilderFactory dbFactoty = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactoty.newDocumentBuilder();
        Document doc = dBuilder.parse(parsingUrl);

        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("item");

        for(int i=0; i<nList.getLength(); i++){
            Node nodeItem = nList.item(i);

            String dutyName = ((Element) nodeItem).getElementsByTagName("dutyName").item(0).getTextContent();
            String dutyTel3 = ((Element) nodeItem).getElementsByTagName("dutyTel3").item(0).getTextContent();
            String hvamyn = ((Element) nodeItem).getElementsByTagName("hvamyn").item(0).getTextContent();
            String hvec = ((Element) nodeItem).getElementsByTagName("hvec").item(0).getTextContent();

            System.out.println("기관이름 = " + dutyName);  //기관이름
            System.out.println("전화번호 = " + dutyTel3);  //전화번호
            System.out.println("구급차가용여부 = " + hvamyn);     //구급차가용여부
            System.out.println("응급실 = " + hvec); //응급실 갯수
            System.out.println("==================================");

            EmergencyRoom er = new EmergencyRoom(dutyName,dutyTel3,hvamyn,hvec);
            erList.add(er);
        }

        model.addAttribute("erList",erList);

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        System.out.println("Response code: " + conn.getResponseCode());
        BufferedReader rd;
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
        }
        rd.close();
        conn.disconnect();
        System.out.println(sb);

        return "question_list";
    }
    /*List<Question> questionList = this.questionService.getList();
    model.addAttribute("questionList", questionList);
    return "question_list";*/

    @RequestMapping("Gyeonggi")
    public String GyeonggiList(){
        System.out.println("Gyeonggi");

        return "question_list";
    }


    @RequestMapping(value = "detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
        Question question = this.questionService.getQuestion(id);
        model.addAttribute("question", question);
        return "question_detail";
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("create")
    public String questionCreate(QuestionForm questionForm) {
        return "question_form";
         }
    @PreAuthorize("isAuthenticated()")
    @PostMapping("create")
    public String questionCreate(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.create(questionForm.getSubject(), questionForm.getContent(), siteUser);
        return "redirect:/question/list";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("modify/{id}")
    public String questionModify(QuestionForm questionForm, @PathVariable("id") Integer id, Principal principal) {
        Question question = this.questionService.getQuestion(id);
        if(!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        return "question_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("modify/{id}")
    public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult,
                                 Principal principal, @PathVariable("id") Integer id) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        return String.format("redirect:/question/detail/%s", id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.questionService.delete(question);
        return "redirect:/";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("vote/{id}")
    public String questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.vote(question, siteUser);
        return String.format("redirect:/question/detail/%s", id);
    }
}