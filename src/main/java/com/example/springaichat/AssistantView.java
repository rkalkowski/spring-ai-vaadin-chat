package com.example.springaichat;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import reactor.core.Disposable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The entire chat UI — no JavaScript, no REST controller, no client-side state.
 * The component tree <em>is</em> the state.
 */
@Route("")
@PageTitle("Docs assistant")
public class AssistantView extends VerticalLayout {

    private final AiChatService chat;
    private final MessageList messageList = new MessageList();
    private final MessageInput input = new MessageInput();
    private final List<MessageListItem> items = new ArrayList<>();

    private Disposable activeStream;

    public AssistantView(AiChatService chat) {
        this.chat = chat;

        messageList.setMarkdown(true);   // model output is Markdown; render it as such
        messageList.setSizeFull();
        input.setWidthFull();
        input.addSubmitListener(event -> ask(event.getValue()));

        addClassName("assistant-view");
        setSizeFull();
        setPadding(true);
        addAndExpand(messageList);
        add(input);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        addMessage("Ask me about the Acme ERP docs — try *\"how do I configure SSO?\"*",
                "Assistant");
    }

    private void ask(String question) {
        addMessage(question, "You");

        var answer = addMessage("", "Assistant");
        input.setEnabled(false);

        // Capture the UI here: the callbacks below run on a Reactor thread, which has
        // no notion of "current UI".
        var ui = UI.getCurrent();

        activeStream = chat.stream(question,
                token -> ui.access(() -> {
                    answer.appendText(token);
                    messageList.setItems(items);
                }),
                () -> ui.access(() -> input.setEnabled(true)));
    }

    private MessageListItem addMessage(String text, String author) {
        var item = new MessageListItem(text, Instant.now(), author);
        items.add(item);
        messageList.setItems(items);
        return item;
    }

    /**
     * Cancel any in-flight completion when the user navigates away or closes the tab.
     * Skipping this is the most common way an LLM feature quietly wastes money.
     */
    @Override
    protected void onDetach(DetachEvent event) {
        if (activeStream != null && !activeStream.isDisposed()) {
            activeStream.dispose();
        }
    }
}
