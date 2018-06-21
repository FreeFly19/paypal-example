package com.spdukraine.paymeplease;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class PayMePleaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayMePleaseApplication.class, args);
    }
}

@Controller
class MainController {
    static final String clientID = "ATs7FMS9FWrxJdeQehZvyz8m07eeqplAP6c90yn1o6p6uyk44yBKXn1M9CMqRUbo3jwu9AlVbBpGIEUf";
    static final String clientSecret = "EAwGSDLp5nvM8Y2c2zNlfYknyAKm94L7Q1BMFpXY6nfTEu71GYbK9RdRxWIFpYbkdpjIPVpHo-50JLNJ";
    static final String mode = "sandbox";
    static final APIContext apiContext = new APIContext(clientID, clientSecret, mode);
    static final String address = "https://bill.freefly19.com";

    List<WebHookDetails> webHooks = new ArrayList<>();


    @PostMapping("pay")
    public String pay(@RequestParam("amount") Integer a) throws PayPalRESTException {

        Details details = new Details();
        details.setSubtotal(a + "");

        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal(a + ""); // Total must be equal to sum of shipping, tax and subtotal.
        amount.setDetails(details);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("This is the payment transaction description.");

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(address + "/paid");
        redirectUrls.setCancelUrl(address + "/");
        payment.setRedirectUrls(redirectUrls);


        Payment createdPayment = payment.create(apiContext);
        Iterator<Links> links = createdPayment.getLinks().iterator();

        while (links.hasNext()) {
            Links link = links.next();

            if (link.getRel().equalsIgnoreCase("approval_url")) {
                return "redirect:" + link.getHref();
            }
        }

        throw new IllegalStateException();
    }

    @GetMapping("paid")
    public String paid(@RequestParam String paymentId, @RequestParam String token, @RequestParam String PayerID) throws PayPalRESTException {
        Payment.get(apiContext, paymentId).execute(apiContext, new PaymentExecution().setPayerId(PayerID));
        System.out.println(Payment.get(apiContext, paymentId));
        return "redirect:/";
    }

    @RequestMapping(value = "web-hook")
    @ResponseBody
    public void webHook(HttpServletRequest request) throws IOException {
        Map<String, Object> map = new ObjectMapper().readValue(request.getInputStream(), Map.class);
        webHooks.add(new WebHookDetails(map.get("summary").toString(), map.get("create_time").toString(), map));
    }

    @GetMapping("events")
    @ResponseBody
    public List<WebHookDetails> events() {
        return webHooks;
    }

    @GetMapping("clear")
    @ResponseBody
    public void clear() {
        webHooks.clear();
    }

}

class WebHookDetails {
    String summary;
    String createdAt;
    Object details;

    public WebHookDetails(String summary, String createdAt, Object details) {
        this.summary = summary;
        this.createdAt = createdAt;
        this.details = details;
    }

    public String getSummary() {
        return summary;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Object getDetails() {
        return details;
    }
}