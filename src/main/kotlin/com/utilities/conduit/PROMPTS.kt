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
}
