package info.blockchain.wallet.contacts.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import io.mikael.urlbuilder.util.UrlParameterMultimap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class Contact {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("company")
    private String company;

    @JsonProperty("email")
    private String email;

    @JsonProperty("xpub")
    private String xpub;

    @JsonProperty("note")
    private String note;

    @JsonProperty("mdid")
    private String mdid;

    @JsonProperty("created")
    private long created;

    @JsonProperty("invitationSent")
    private String invitationSent; // I invited somebody

    @JsonProperty("invitationReceived")
    private String invitationReceived;// Somebody invited me

    @JsonProperty("facilitatedTxList")
    private HashMap<String, FacilitatedTransaction> facilitatedTransaction;

    public Contact() {
        this.id = UUID.randomUUID().toString();
        this.facilitatedTransaction = new HashMap<>();
        this.created = System.currentTimeMillis() / 1000;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getXpub() {
        return xpub;
    }

    public void setXpub(String xpub) {
        this.xpub = xpub;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getMdid() {
        return mdid;
    }

    public void setMdid(String mdid) {
        this.mdid = mdid;
    }

    public String getInvitationSent() {
        return invitationSent;
    }

    public void setInvitationSent(String invitationSent) {
        this.invitationSent = invitationSent;
    }

    public String getInvitationReceived() {
        return invitationReceived;
    }

    public void setInvitationReceived(String invitationReceived) {
        this.invitationReceived = invitationReceived;
    }

    @Nonnull
    public HashMap<String, FacilitatedTransaction> getFacilitatedTransactions() {
        return facilitatedTransaction != null
                ? facilitatedTransaction : new HashMap<String, FacilitatedTransaction>();
    }

    public void addFacilitatedTransaction(FacilitatedTransaction facilitatedTransaction) {
        this.facilitatedTransaction.put(facilitatedTransaction.getId(), facilitatedTransaction);
    }

    public void deleteFacilitatedTransaction(String fctxId) {
        facilitatedTransaction.remove(fctxId);
    }

    public void setFacilitatedTransactions(HashMap<String, FacilitatedTransaction> facilitatedTransaction) {
        this.facilitatedTransaction = facilitatedTransaction;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public Contact fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, Contact.class);
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    private UrlParameterMultimap toQueryParameters() {
        UrlParameterMultimap queryParams = UrlParameterMultimap.newMultimap();
        if (id != null) queryParams.add("id", invitationSent);
        if (name != null) queryParams.add("name", name);
        if (surname != null) queryParams.add("surname", surname);
//        if (company != null) queryParams.add("company", company);
//        if (email != null) queryParams.add("email", email);
//        if (note != null) queryParams.add("note", note);
//        if (xpub != null) queryParams.add("xpub", xpub);
//        if (mdid != null) queryParams.add("mdid", mdid);

        return queryParams;
    }

    public Contact fromQueryParameters(Map<String, String> queryParams) {
        Contact contact = new Contact();
        contact.invitationReceived = queryParams.get("id");
        contact.name = queryParams.get("name");
        contact.surname = queryParams.get("surname");

        return contact;
    }

    public String createURI() throws URISyntaxException {
        UrlParameterMultimap urlParameterMultimap = toQueryParameters();

        UrlBuilder urlBuilder = UrlBuilder.empty()
                .withScheme("https")
                .withHost("blockchain.info")
                .withPath("/invite")
                .withParameters(urlParameterMultimap);

        return urlBuilder.toUri().toString();
    }
}
