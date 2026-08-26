package com.utilities.conduit

object PROMPTS {

    val TITLE_GENERATION = """
        Generate a concise, descriptive title for this conversation.
        
        If the current title is already accurate and concise, return it unchanged.
        Otherwise, improve it while preserving the original intent whenever possible.
        
        Requirements:
        - Respond with the title only.
        - Use plain text.
        - Do not use quotation marks.
        - Do not end the title with punctuation.
        - Keep the title under ten words.
        
         The current title is:
        {CURRENT_TITLE}
        
        Preceding context:
        {PRECEDING_CONTEXT}
        
    """.trimIndent()

    val HISTORY_SUMMARY_GENERATION = """
        Create a concise summary of the conversation up to and including the final
        message in the conversation below.
    
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
        - Do not use quotation marks.
        - Do not mention that you are generating a summary.
        - Be concise while preserving important context.
       
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
        - Update or correct existing information when the new information supersedes it.
        - Remove information that is clearly no longer valid.
        - Do not simply append the new information to the previous model.
        - Do not mention this instruction, the previous model, or the source conversations.
        - Do not invent information or infer unsupported personal facts.
        - Keep the model concise and focused on information likely to be useful in future conversations.
        - Output only the resulting user model.
    """.trimIndent()

    val CHAT_SUMMARY_GENERATION = """
        Create a concise summary of the conversation below for the purpose of
        identifying information that may be useful in building a persistent model
        of the user.
    
        Focus on information about the user that is likely to remain useful across
        future conversations, including:
        - user preferences and requirements
        - important personal facts explicitly provided by the user
        - ongoing projects, goals, plans, and commitments
        - technical environment, tools, and workflows
        - recurring interests or patterns
        - decisions that reveal durable preferences or requirements
    
        Do not summarize every exchange or preserve ordinary conversational detail.
        Focus on information that could help an assistant better understand and
        assist this user in future conversations.
    
        Requirements:
        - Respond with the summary only.
        - Do not use quotation marks.
        - Do not mention that you are generating a summary.
        - Do not invent information or infer unsupported personal facts.
        - Be concise while preserving useful information.
        
        Conversation:
        
        {CONVERSATION}
""".trimIndent()

}
