#!/usr/bin/env python3
#
# Usage:
#    python3 gmail_to_conduit.py email-conversation.txt -o email-conversation-conduit.json
#
#    (Then copy email-conversation-conduit.json to .../Conduit/chats/ - You can then
#    navigate the email convo tree graphically and teleport from one message to another
#    like in a regular Conduit chat)
#
# ============================================================================
# AI MAINTENANCE PROMPT — GMAIL -> CONDUIT IMPORTER
# ============================================================================
#
# Please review the complete Python script below.
#
# This script was written by ChatGPT in September 2026 to convert a downloaded
# Gmail conversation into a conversational tree represented as a Conduit Chat
# JSON file. The resulting JSON can be loaded directly into Conduit and viewed
# and navigated as a Conduit-style branching conversation.
#
# IMPORTANT SOURCE-FORMAT NOTE:
# The current script does NOT call the Gmail API directly. It parses the XML-like
# Gmail print-view/download format represented by the accompanying example file
# (fcuemail_sanitized.txt). Therefore, when reviewing it, distinguish between:
#   1. the Gmail API, if relevant to obtaining messages in the future; and
#   2. the Gmail print-view/export format that this script actually parses.
# Verify both where appropriate, but do not invent an API dependency that the
# script does not have.
#
# TASK
# ----
# Please determine whether this script is still functional and appropriate for
# the current Gmail environment as of today.
#
# 1. Inspect the entire script carefully.
# 2. Check current Google/Gmail documentation and other authoritative current
#    sources as necessary. In particular, determine whether Gmail's current
#    downloadable/print-view conversation representation still has the fields,
#    XML-like structure, quoting behavior, attachment representation, and
#    encoding assumptions relied upon here.
# 3. If Google has changed the relevant Gmail format or API behavior since this
#    script was written, identify the concrete incompatibilities and update the
#    script accordingly.
# 4. If the current script remains valid, do not change it merely for style.
#    Make only changes that improve correctness, robustness, compatibility, or
#    handling of demonstrable current Gmail behavior.
# 5. Preserve the script's command-line usability. It should continue to accept
#    an input Gmail export file and optionally an output path, and should produce
#    a Conduit Chat JSON file.
# 6. The output must remain deterministic and valid JSON. Use UTF-8 and preserve
#    Unicode email content correctly.
# 7. Do not introduce unnecessary third-party Python dependencies. Prefer the
#    Python standard library unless a dependency is genuinely required.
# 8. Handle malformed or unexpected input defensively, with useful error
#    messages rather than silently producing corrupt Conduit data.
# 9. Preserve the reconstruction strategy unless current evidence shows that a
#    better strategy is necessary. In particular, do not invent Gmail message
#    IDs when they are unavailable in the downloaded source. The script should
#    reconstruct parent/child relationships from the available message order,
#    sender, recipients, timestamps, and conversation structure.
# 10. Multiple legitimate roots are allowed. They must be represented beneath
#     the synthetic "Start of Thread" node rather than forcing an invented
#     parent relationship between unrelated messages.
# 11. The synthetic "Start of Thread" node must be a TEXT node, not an INFO node,
#     because INFO is deprecated in the current Conduit model.
# 12. The synthetic "Start of Thread" node must be the Chat cursor when the
#     imported chat is opened.
# 13. The Gmail conversation subject should become the Conduit Chat title.
# 14. Messages sent by the account owner should use AuthorType.USER.
# 15. Messages sent by other participants should use AuthorType.ASSISTANT, and
#     their individual message title should contain the sender's display name and
#     a human-readable date, e.g. "Alex Morgan - Jul 11, 2026".
# 16. Every imported email should become a TEXT node. Do not use INFO nodes for
#     imported email messages.
# 17. Preserve chronological order when determining relationships and retain
#     timestamps in the Conduit JSON.
# 18. Preserve CC recipients insofar as they are useful to the reconstruction,
#     but do not put email headers into the message body unless the source
#     actually contains them.
# 19. Do not expose or manufacture private information. The script should simply
#     convert whatever data the user supplies; it should not log message bodies,
#     credentials, tokens, or other sensitive content unnecessarily.
#
# CONDUIT TARGET STRUCTURE
# ------------------------
# The output JSON must match the following Kotlin data model used by Conduit:
#
# @Serializable
# data class Chat(
#     val id: String,
#     val createdAt: Long = System.currentTimeMillis(),
#     var rootNodeId: String?,
#     var cursorNodeId: String? = null,
#     var title: String,
#     var needsHumanReview: Boolean = false,
#     val nodes: MutableMap<String, Node> = mutableMapOf(),
# )
#
# @Serializable
# data class Node(
#     val id: String,
#     val type: NodeType,
#     val createdAt: Long = System.currentTimeMillis(),
#     val parentId: String?,
#     val children: MutableList<String> = mutableListOf(),
#     val message: ChatMessage?,
#     var historySummary: String? = null
# )
#
# @Serializable
# enum class NodeType { TEXT, INFO }
#
# @Serializable
# data class ChatMessage(
#     var title: String? = null,
#     val timestamp: Long = System.currentTimeMillis(),
#     var status: MessageStatus = MessageStatus.COMPLETE,
#     val authorType: AuthorType = AuthorType.INTERNAL,
#     var text: String,
#     var responseTime: Long? = null
# )
#
# enum class MessageStatus { COMPLETE, INTERRUPTED, ERROR }
# enum class AuthorType { USER, ASSISTANT, INTERNAL }
#
# EXPECTED JSON SEMANTICS
# -----------------------
# A valid output should therefore have this conceptual form:
#
# {
#   "id": "<chat id>",
#   "createdAt": <long>,
#   "rootNodeId": "<start-of-thread node id>",
#   "cursorNodeId": "<same start-of-thread node id>",
#   "title": "<Gmail subject>",
#   "needsHumanReview": false,
#   "nodes": {
#     "<node id>": {
#       "id": "<node id>",
#       "type": "TEXT",
#       "createdAt": <long>,
#       "parentId": "<parent id or null>",
#       "children": ["<child id>", "..."],
#       "message": {
#         "title": "<sender name> - <Mon D, YYYY>",
#         "timestamp": <long>,
#         "status": "COMPLETE",
#         "authorType": "USER|ASSISTANT|INTERNAL",
#         "text": "<message text>",
#         "responseTime": null
#       },
#       "historySummary": null
#     }
#   }
# }
#
# The synthetic Start of Thread node should have parentId == null, type == TEXT,
# and be the Chat's rootNodeId and cursorNodeId. Its message may contain the
# synthetic marker text/name used by the current implementation, but it must be
# structurally valid as a normal TEXT node.
#
# OUTPUT REQUIREMENT
# ------------------
# After reviewing the script and current Gmail behavior, output the complete,
# corrected, functional Python script — not a patch, not a diff, and not merely
# recommendations. The returned script must be self-contained and ready to save
# as gmail_to_conduit.py and run immediately.
#
# If no code changes are required, still output the complete script exactly as
# it should be used today. Briefly state what you verified before the script,
# but make the complete corrected script the primary deliverable.
#
# ============================================================================

"""Convert a Gmail thread export (Conduit email_thread XML) to a Conduit Chat JSON.

Usage:
    python3 gmail_to_conduit.py fcuemail.txt -o fcuemail-conduit.json

The Gmail export does not contain Gmail Message-IDs.  Parentage is therefore
reconstructed from chronology and sender/recipient relationships.  Messages
which cannot be attached to an earlier message are attached to one synthetic
"Start of Thread" node, giving Conduit exactly one root.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import uuid
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional


# Change this only if you want a different sender to be treated as the user.
USER_EMAIL = "venkataraman.anand@gmail.com"


def new_id() -> str:
    return str(uuid.uuid4())


def epoch_ms(iso_date: str) -> int:
    dt = datetime.fromisoformat(iso_date.replace("Z", "+00:00"))
    return int(dt.timestamp() * 1000)


def normalize_email(value: str | None) -> str:
    return (value or "").strip().lower()


def clean_text(value: str | None) -> str:
    # ElementTree leaves CDATA as ordinary text. Preserve the message itself,
    # but remove the surrounding whitespace introduced by the XML export.
    return (value or "").strip()


def first_line(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def human_date(iso_date: str) -> str:
    """Format an ISO timestamp as a concise human-readable date."""
    dt = datetime.fromisoformat(iso_date.replace("Z", "+00:00"))
    return dt.strftime("%b %-d, %Y")


@dataclass
class Email:
    n: int
    date: str
    sender_name: str
    sender_email: str
    to: list[str]
    cc: list[str]
    body: str
    parent: Optional["Email"] = None
    node_id: Optional[str] = None

    @property
    def timestamp(self) -> int:
        return epoch_ms(self.date)

    @property
    def recipients(self) -> set[str]:
        return set(self.to + self.cc)


def parse_export(path: Path) -> tuple[str, list[Email]]:
    tree = ET.parse(path)
    root = tree.getroot()
    title = root.findtext("./meta/subject") or "Imported Gmail Thread"

    emails: list[Email] = []
    for element in root.findall("message"):
        sender = element.attrib
        to = [
            normalize_email(x.attrib.get("email"))
            for x in element.findall("./to/recipient")
            if x.attrib.get("email")
        ]
        cc = [
            normalize_email(x.attrib.get("email"))
            for x in element.findall("./cc/recipient")
            if x.attrib.get("email")
        ]
        body = clean_text(element.findtext("body"))

        emails.append(
            Email(
                n=int(element.attrib["n"]),
                date=element.attrib["date"],
                sender_name=element.attrib.get("from", "Unknown sender"),
                sender_email=normalize_email(element.attrib.get("email")),
                to=to,
                cc=cc,
                body=body,
            )
        )

    # The export's n is normally chronological, but sort explicitly so the
    # reconstruction does not depend on document ordering.
    emails.sort(key=lambda e: (e.timestamp, e.n))
    return title, emails


def parent_score(candidate: Email, current: Email) -> int:
    """Return a relationship score; zero means 'not a plausible parent'.

    The important rule is that the candidate must have addressed the current
    sender.  Among such candidates, the most recent one wins.  A same-sender
    candidate is deliberately allowed: Gmail exports can contain consecutive
    messages from the user before a reply arrives.
    """
    if current.sender_email not in candidate.recipients:
        return 0

    score = 100

    # A different sender is a somewhat stronger conversational signal, but
    # chronology remains the primary tie-breaker.
    if candidate.sender_email != current.sender_email:
        score += 10

    # If the current message is addressed to the candidate sender, that is
    # additional evidence of a direct continuation.
    if candidate.sender_email in current.recipients:
        score += 5

    return score


def reconstruct(emails: list[Email]) -> Email:
    """Assign every email a parent, using a synthetic global root as needed."""
    root = Email(
        n=0,
        date=emails[0].date if emails else datetime.now(timezone.utc).isoformat(),
        sender_name="Start of Thread",
        sender_email="",
        to=[],
        cc=[],
        body="",
    )
    root.node_id = new_id()

    for index, current in enumerate(emails):
        best: Optional[Email] = None
        best_score = 0

        # Search backwards. The nearest plausible predecessor is preferred.
        for candidate in reversed(emails[:index]):
            score = parent_score(candidate, current)
            if score == 0:
                continue
            if best is None or score > best_score:
                best = candidate
                best_score = score
                # A direct reciprocal relationship is the strongest signal;
                # because we're scanning newest-first, we can stop here.
                if score >= 115:
                    break

        current.parent = best if best is not None else root
        current.node_id = new_id()

    return root


def make_chat(title: str, emails: list[Email], root: Email) -> dict:
    nodes: dict[str, dict] = {}

    # Synthetic structural root. It is a TEXT node so it works with the
    # current Conduit model, but its message is marked INTERNAL and can be
    # filtered from LLM history if desired.
    nodes[root.node_id] = {
        "id": root.node_id,
        "type": "TEXT",
        "createdAt": root.timestamp,
        "parentId": None,
        "children": [],
        "message": {
            "title": "Start of Thread",
            "timestamp": root.timestamp,
            "status": "COMPLETE",
            "authorType": "INTERNAL",
            "text": "Start of Thread",
            "responseTime": None,
        },
        "historySummary": None,
    }

    for email in emails:
        assert email.node_id is not None
        assert email.parent is not None
        assert email.parent.node_id is not None

        author_type = "USER" if email.sender_email == USER_EMAIL else "ASSISTANT"
        node = {
            "id": email.node_id,
            "type": "TEXT",
            "createdAt": email.timestamp,
            "parentId": email.parent.node_id,
            "children": [],
            "message": {
                "title": f"{email.sender_name} - {human_date(email.date)}",
                "timestamp": email.timestamp,
                "status": "COMPLETE",
                "authorType": author_type,
                "text": email.body,
                "responseTime": None,
            },
            "historySummary": None,
        }
        nodes[email.node_id] = node
        nodes[email.parent.node_id]["children"].append(email.node_id)

    return {
        "id": new_id(),
        "createdAt": root.timestamp,
        "rootNodeId": root.node_id,
        "cursorNodeId": root.node_id,
        "title": title,
        "needsHumanReview": False,
        "nodes": nodes,
    }


def print_tree(node_id: str, nodes: dict, prefix: str = "", last: bool = True) -> None:
    node = nodes[node_id]
    message = node.get("message")
    if message:
        title = message.get("title") or "?"
        text = first_line(message.get("text", ""))
        if len(text) > 65:
            text = text[:62] + "..."
        print(f"{prefix}{'└── ' if last else '├── '}{title}: {text}")
    else:
        print(f"{prefix}{'└── ' if last else '├── '}[no message]")

    child_prefix = prefix + ("    " if last else "│   ")
    children = node.get("children", [])
    for i, child in enumerate(children):
        print_tree(child, nodes, child_prefix, i == len(children) - 1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("-o", "--output", type=Path)
    args = parser.parse_args()

    try:
        title, emails = parse_export(args.input)
    except (ET.ParseError, OSError, KeyError, ValueError) as exc:
        print(f"Error reading {args.input}: {exc}", file=sys.stderr)
        return 1

    if not emails:
        print("No messages found.", file=sys.stderr)
        return 1

    root = reconstruct(emails)
    chat = make_chat(title, emails, root)

    output = args.output or args.input.with_name(args.input.stem + "-conduit.json")
    output.write_text(json.dumps(chat, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    root_children = chat["nodes"][chat["rootNodeId"]]["children"]
    print(f"Converted {len(emails)} messages -> {output}")
    print(f"Chat title: {title}")
    print(f"Synthetic root children: {len(root_children)}")
    print("\nReconstructed tree:")
    print_tree(chat["rootNodeId"], chat["nodes"])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

