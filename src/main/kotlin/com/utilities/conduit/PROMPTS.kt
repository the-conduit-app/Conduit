package com.utilities.conduit

object PROMPTS {

    val TITLE_GENERATION = """
        Current chat title:
    
        {CURRENT_TITLE}
    
        Based on the conversation and context above, generate a concise,
        descriptive title for the conversation.
    
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
        Create a concise summary of the conversation above, up to the final
        message in that conversation.
    
        The new summary will be used as persistent context when continuing the
        conversation from this point. Preserve the information that would be
        important for a future continuation, including:
        - important facts and conclusions
        - decisions and their reasoning
        - questions and unresolved issues
        - user preferences and requirements
        - relevant plans or commitments
        - important technical or contextual details
    
        Do not merely describe the conversation. Produce a useful continuation
        context that allows another response to pick up naturally from this point.
    
        Requirements:
        - Respond with the summary only.
        - Use up to 500 words.
        - Do not use quotation marks.
        - Do not mention that you are generating a summary.
        - Be concise while preserving important context.
    """.trimIndent()

    val CHAT_SUMMARY_GENERATION = """
        Create a concise summary of the conversation below for the purpose of
        identifying information that may be useful in building a persistent model
        of the user.
    
        Focus on information about the user that is likely to remain useful across
        future conversations, including:
        - user preferences and requirements
        - important user-provided facts that are likely to be useful in future conversations
        - ongoing projects, goals, plans, and commitments
        - technical environment, tools, and workflows
        - recurring interests or patterns
        - decisions that reveal durable preferences or requirements
    
        Some long messages may have been truncated. Do not assume that the
        truncated portions contain any particular information.
    
        Do not summarize every exchange or preserve ordinary conversational detail.
        Focus on information that could help an assistant better understand and
        assist this user in future conversations.
    
        The summary must represent the conversation as a whole, not merely its
        opening exchange or the assistant's answer to the first question.
    
        Importantly, trace the user's conversation across all messages and identify
        information that is useful for understanding the user. Give substantially
        more weight to what the user says, asks, prefers, decides, or reveals than
        to factual information supplied by the assistant.
    
        Requirements:
        - Respond with the summary only. Do not produce an answer to user questions.
        - Do not use quotation marks.
        - Do not mention that you are generating a summary.
        - Do not invent information or infer unsupported personal facts.
        - Be concise while preserving useful information.
        - Use up to about a thousand words in total
        - IMPORTANT: If a previous summary is provided, preserve its useful information and
          incorporate any new information from the conversation. Do not discard
          useful information merely because it is not repeated in the current branch of
          the conversation.

        Previous Summary:
        
        {PREVIOUS_SUMMARY}
    """.trimIndent()

    val USER_MODEL_GENERATION = """
    You maintain a concise, accurate model of the user based on information learned from their conversations.

    The previous known model of the user is:

    {EXISTING_USER_MODEL}

    Since that model was generated, the following new information has become available from one or more conversations:

    {NEW_INFORMATION}

    Generate a new user model by incorporating the new information into the previous model.

    Requirements:
    - Preserve accurate information from the previous model.
    - Incorporate genuinely useful new information.
    - Be Verbose. Use up to 2000 words if necessary.
    - Update or correct existing information when the new information supersedes it.
    - Remove information that is clearly no longer valid.
    - Do not simply append the new information to the previous model.
    - Do not mention this instruction, the previous model, or the source conversations.
    - Do not invent information or infer unsupported personal facts.
    - Keep the model concise and focused on information likely to be useful in future conversations.
    - Output only the resulting user model.
""".trimIndent()
}
