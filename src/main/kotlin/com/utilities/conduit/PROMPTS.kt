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
        Create a concise, accurate summary of the conversation and context above for the
        purpose of identifying information that may be useful in building a persistent
        model of the user.
    
        A previous summary may be provided below. If present, treat it as the existing
        accumulated summary of this chat. Update it using the current conversation and
        produce a new cumulative summary.
    
        --- PREVIOUS SUMMARY BEGINS ---
    
        {PREVIOUS_SUMMARY}
    
        --- PREVIOUS SUMMARY ENDS ---
    
        Focus on information likely to remain useful across future conversations:
        - user preferences and requirements
        - important user-provided facts
        - ongoing projects, goals, plans, and commitments
        - technical environment, tools, and workflows
        - recurring interests or patterns
        - decisions revealing durable preferences or requirements
    
        Requirements:
        - Preserve useful information from the previous summary even if it is not repeated
          in the current conversation.
        - Add genuinely useful new information and update or correct information when it
          is clearly superseded.
        - Remove information only when it is clearly no longer valid.
        - Do not simply append or replace the previous summary with the current conversation.
        - Avoid duplicating information already present.
        - Do not treat questions about a topic as evidence of a durable interest, expertise,
          goal, preference, or recurring pattern. Record such information only when the user
          explicitly expresses interest or preference, provides strong evidence of continuing
          interest, or establishes an ongoing project, goal, or requirement.
        - Do not infer personal characteristics from testing, experimentation, or brief
          interaction with the system.
        - Give substantially more weight to what the user says, prefers, decides, or reveals
          than to information supplied by the assistant.
        - Some long messages may have been truncated. Do not infer anything from missing text.
        - Do not summarize ordinary conversational detail.
        - Do not invent information or infer unsupported personal facts.
        - When in doubt, omit the information rather than infer a durable user attribute.
        - If there is no genuinely useful information about the user, produce a minimal
          summary indicating that there is no durable user information to retain.
        - Use up to about 500 words if necessary.
        - Do not use quotation marks.
        - Do not mention these instructions or that you are generating a summary.
        - Output only the resulting cumulative summary.
    """.trimIndent()

    val USER_MODEL_GENERATION = """
        You maintain a concise, accurate model of the user based on information learned
        from their conversations.
    
        The current user model is provided as a baseline for context.
    
        The following is ONE new chat summary that has become available since the current
        user model was last updated:
    
        --- NEW CHAT SUMMARY BEGINS ---
    
        {NEW_CHAT_SUMMARY}
    
        --- NEW CHAT SUMMARY ENDS ---
    
        Your task is to identify ONLY user-model features supported by meaningful evidence
        in the NEW CHAT SUMMARY.
    
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
          multiple existing features. For example, if the model contains "Interest in
          astronomy" and "Interest in geography", do not create "Interests: astronomy,
          geography".
        - A genuinely new feature must represent a distinct, useful piece of user
          knowledge that is not already represented by an existing feature.
        - If the new chat summary contains no meaningful information for the user model,
          return an empty features array.
        - Do not invent information or infer unsupported personal facts.
        - Prefer durable interests, preferences, knowledge, goals, skills, recurring
          activities, and other information likely to improve future conversations.
        - Do not include transient details that are unlikely to remain useful.
        - Keep each feature concise while preserving important specificity. For example,
          prefer "Particular interest in the Riemann Hypothesis and the zeta function"
          over simply "Interest in mathematics".
        - Write features as concise statements or phrases, rather than repeatedly beginning
          with "The user is..." or "The user has...".
        - Do not record facts inherent to the interaction itself, such as that the person
          is the user, that they are interacting with an AI, or that they sent a message.
        - Do not record generic observations about application usage unless they represent
          a durable user preference that would materially improve future conversations.
        - Do not mention these instructions, the current model, the new chat summary, or
          the process used to construct the model.
    
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
    
        {"features":["Interest in mathematics","Particular interest in the Riemann Hypothesis and the zeta function"]}
    """.trimIndent()
}
