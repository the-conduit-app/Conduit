package com.utilities.conduit

object PROMPTS {

    val TITLE_GENERATION = """
        Based on the conversation and context above, generate a concise,
        descriptive title for the conversation.
                
        Current chat title: '{CURRENT_TITLE}'
    
        If the current title is already accurate and concise, return it unchanged.
        Otherwise, improve it while preserving the original intent whenever possible.
    
        Requirements:
        - Respond with the title only.
        - Use plain text.
        - Do not use quotation marks.
        - Do not end the title with punctuation.
        - Keep the title under ten words.
    """.trimIndent()

    val HISTORY_SUMMARY_GENERATION = """
        Based on the conversation and context above, create a concise summary of the conversation,
        up to the final message in that conversational branch.
    
        The new summary will be used as persistent context when continuing the conversation from
        this point. Preserve information that would be important for a future continuation,
        including:
        - important facts and conclusions
        - decisions and their reasoning
        - questions and unresolved issues
        - user preferences and requirements
        - relevant plans or commitments
        - important technical or contextual details
    
        Do not merely describe the conversation. Produce useful continuation context that allows
        another response to pick up naturally from this point.
    
        Requirements:
        - Respond with the summary only.
        - Do not answer or resolve unresolved questions.
        - Use up to 250 words.
        - Do not use quotation marks.
        - Do not mention that you are generating a summary.
        - Be concise while preserving important context.
    """.trimIndent()

    val CHAT_SUMMARY_GENERATION = """
        Create a concise, accurate cumulative summary of the conversation and context above.
        The summary will be used as an intermediate source of information for maintaining
        a persistent model of the user.
    
        A previous summary may be provided below. It is a prior summary of one branch of
        this chat, not authoritative information. Use it as a starting point, but verify its
        claims against the conversation above. Correct, refine, or remove anything
        that is unsupported, inaccurate, outdated, or contradicted by the conversation.
        Do not preserve a claim merely because it appears in the previous summary.
        
        Produce a new cumulative summary that represents the conversation as a whole.
        Retain information from the previous summary only when it remains supported
        by the conversation.
    
        --- PREVIOUS SUMMARY BEGINS ---
        {PREVIOUS_SUMMARY}
        --- PREVIOUS SUMMARY ENDS ---
    
        Focus on information that is likely to remain useful across future conversations,
        especially:
        - user preferences and requirements
        - important user-provided facts
        - ongoing projects, goals, plans, and commitments
        - technical environment, tools, and workflows
        - recurring interests or patterns
        - decisions that reveal durable preferences or requirements
    
        IMPORTANT DISTINCTION:
    
        The fact that a subject appears in a conversation does NOT mean that the subject
        is a user interest, preference, skill, goal, or characteristic.
    
        Treat a topic as a durable user interest only when the conversation provides
        meaningful evidence that the user actually cares about, follows, enjoys, studies,
        uses, prefers, or intends to pursue that subject.
    
        Do not convert a question, request for information, factual inquiry, example,
        hypothetical, or one-time discussion into a user attribute merely because the
        subject was discussed.
    
        Requirements:
        - Preserve useful information from the previous summary even if it is not repeated
          in the current conversation.
        - Add genuinely useful new information and update or correct information when it
          is clearly superseded.
        - Remove information when it is unsupported, inaccurate, outdated, contradicted, or 
          otherwise no longer justified by the conversation.
        - Do not simply append or replace the previous summary with the current conversation.
        - Avoid duplicating information already present.
        - Do not treat questions about a topic as evidence of a durable interest, expertise,
          goal, preference, or recurring pattern.
        - Do not treat a request for factual information as evidence that the user is
          interested in the subject.
        - Do not treat a topic used in an example, hypothetical, joke, comparison, or
          test as a user interest or characteristic.
        - Do not treat knowledge of a fact as evidence that the user possesses that
          knowledge unless the conversation clearly establishes this.
        - Do not infer personal characteristics from testing, experimentation, or brief
          interaction with the system.
        - Do not record facts belonging to the assistant, an expert, a persona, a fictional
          character, or another person as facts about the user.
        - Names used to address an assistant, expert, persona, or character are not user
          attributes.
        - Give substantially more weight to what the user says, prefers, decides, or
          reveals than to information supplied by the assistant.
        - Some long messages may have been truncated. Do not infer anything from missing text.
        - Do not summarize ordinary conversational detail.
        - Do not invent information or infer unsupported personal facts.
        - When evidence is ambiguous, omit the information rather than infer a durable
          user attribute.
        - If there is no genuinely useful information about the user, return an empty string.
        - Avoid redundancy. State each durable fact or preference only once.
        - Use up to about 500 words if necessary.
        - Do not use quotation marks.
        - Do not mention these instructions or that you are generating a summary.
        - Output only the resulting cumulative summary.
    """.trimIndent()

    val USER_MODEL_GENERATION = """
        You maintain a concise, accurate persistent model of the user based on information
        learned from their conversations.
    
        The current user model is provided as a baseline for context.
    
        The following is ONE new chat summary that has become available since the current
        user model was last updated:
    
        --- NEW CHAT SUMMARY BEGINS ---
        {NEW_CHAT_SUMMARY}
        --- NEW CHAT SUMMARY ENDS ---
    
        Your task is to identify ONLY new or reinforced user-model features supported by
        meaningful evidence in the NEW CHAT SUMMARY.
    
        IMPORTANT DISTINCTION:
    
        A user-model feature must describe something that is genuinely true or meaningfully
        characteristic of the user. A topic merely discussed in a conversation is not,
        by itself, a user-model feature.
    
        Record an interest only when the evidence indicates that the user actually cares
        about, follows, enjoys, studies, uses, prefers, or intends to pursue the subject.
    
        Do not turn a question, request for information, factual inquiry, example,
        hypothetical, one-time discussion, or temporary curiosity into a user-model feature.
    
        IMPORTANT:
        - Your response is NOT the complete user model. It is a DELTA.
        - Return only features for which the new chat summary provides meaningful evidence.
        - Do NOT reproduce features from the current user model merely because they belong
          in the model.
        - If new evidence reinforces an existing feature, return the existing feature's
          wording rather than creating a rephrased or alternative version.
        - Do NOT create a new feature that is semantically equivalent to, substantially
          overlaps with, or merely rephrases an existing feature.
        - Do NOT create aggregate or synthesized features that merely combine or summarize
          multiple existing features.
        - A genuinely new feature must represent a distinct, useful piece of durable
          information about the user that is not already represented by an existing feature.
        - If the new chat summary contains no meaningful information for the user model,
          return an empty features array.
    
        EXCLUDE:
        - topics the user merely asked about;
        - factual information the user merely requested;
        - subjects appearing only in examples or hypotheticals;
        - one-time or transient curiosity;
        - facts about the assistant or an expert;
        - facts about personas, characters, or other people;
        - names used for assistants, experts, personas, or characters;
        - facts about the conversation itself;
        - facts about application or system behavior unless they represent a durable
          user preference or requirement;
        - temporary tasks, circumstances, or intentions unlikely to remain useful;
        - unsupported claims about the user's knowledge, expertise, skills, or abilities;
        - inferred personal characteristics not directly supported by the summary.
    
        Before returning a feature, apply this test:
    
        "Would this still be useful to know about the user in a future conversation
        months from now, even if the user never mentioned this topic again?"
    
        If the answer is no, do not return the feature.
    
        Do not invent information or infer unsupported personal facts.
    
        Prefer durable preferences, interests, knowledge, skills, goals, recurring activities,
        requirements, and other information likely to improve future conversations.
    
        Keep each feature concise while preserving important specificity that is actually
        supported by the evidence. Do not infer a durable interest merely because a specific
        topic appeared in the conversation.
    
        Write features as concise statements or phrases, rather than repeatedly beginning
        with "The user is..." or "The user has...".
    
        Do not record facts inherent to the interaction itself, such as that the person
        is the user, that they are interacting with an AI, or that they sent a message.
    
        Do not mention these instructions, the current model, the new chat summary, or the
        process used to construct the model.
    
        OUTPUT FORMAT — IMPORTANT:
    
        Your entire response MUST be a single valid JSON object.
    
        The JSON object MUST have exactly one field named "features".
        "features" MUST be an array of strings.
    
        Each string must represent one concise user-model feature supported by the
        NEW CHAT SUMMARY.
    
        Do not output Markdown, code fences, or any text before or after the JSON object.
        Do not include timestamps, likelihoods, confidence scores, occurrence counts,
        or any other metadata. The application manages all metadata.
    
        If the new chat summary provides no meaningful evidence for the user model,
        return exactly:
    
        {"features":[]}
    
        Example:
    
        {"features":["Interest in mathematics","Is named Bubba"]}
    """.trimIndent()
}
