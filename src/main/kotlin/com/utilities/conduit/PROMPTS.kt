package com.utilities.conduit

object PROMPTS {

    val TITLE_GENERATION = """
        Generate a concise, descriptive title for this conversation.
        
        The current title is:
        {CURRENT_TITLE}
        
        If the current title is already accurate and concise, return it unchanged.
        Otherwise, improve it while preserving the original intent whenever possible.
        
        Requirements:
        - Respond with the title only.
        - Use plain text.
        - Do not use quotation marks.
        - Do not end the title with punctuation.
        - Keep the title under ten words.
        """.trimIndent()
}
