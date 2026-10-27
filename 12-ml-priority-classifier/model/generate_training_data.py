"""
Generates a synthetic but realistically-structured dataset of support tickets labeled
by priority. There's no real historical ticket data to train on in this environment,
so this script builds a stand-in with the same shape a real dataset would have:
genuine, learnable signal (urgency language, plan tier, repeat-contact pattern) mixed
with deliberate label noise, so the model that trains on it has something real to
learn without reaching a suspicious 100% accuracy - a classifier that's ever exactly
right is almost always a sign of a leaked label or an unrealistic dataset, not a good
model, and that's worth students seeing directly rather than being told about.
"""

import random
import pandas as pd

random.seed(42)

PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"]

# Tone phrases: the primary signal the model has to learn to read.
TONE_PHRASES = {
    "URGENT": [
        "This is completely unacceptable and needs to be fixed right now.",
        "We are losing money every minute this stays down.",
        "This is the third time this has happened this month.",
        "Our entire team is completely blocked and cannot work.",
        "I need this escalated immediately, this cannot wait.",
        "This is a critical outage affecting our whole workspace.",
    ],
    "HIGH": [
        "This is affecting our work and we'd like it looked at today.",
        "This has been going on for a while now and needs attention soon.",
        "We really need a resolution in the next day or two.",
        "This is becoming a real problem for our team.",
        "Could someone prioritize this, it's holding up a deadline.",
    ],
    "MEDIUM": [
        "Could you help me understand why this is happening.",
        "I'd like this looked at when someone has a chance.",
        "This isn't blocking me but I'd like it resolved.",
        "Running into an issue here and would appreciate some help.",
    ],
    "LOW": [
        "Just a quick question about how this works.",
        "No rush at all, whenever you get a chance.",
        "I was curious about this, not urgent.",
        "Small thing I noticed, figured I'd mention it.",
        "Wondering if this is expected behavior or not.",
    ],
}

# Topic phrases: add realistic vocabulary variety. Deliberately NOT correlated with
# priority in this generator - a billing question can be calm or urgent, same as a
# login problem - so the model has to learn that priority comes from tone/context,
# not from which feature of the product someone is asking about.
TOPIC_PHRASES = [
    "I can't log into my account.",
    "My password reset email never arrived.",
    "The shared folder isn't showing up for my teammate.",
    "I was charged twice on my last invoice.",
    "Can I get a refund for last month's charge.",
    "We're close to our storage limit and need more space.",
    "Uploads keep failing partway through.",
    "Could you add support for larger file uploads.",
    "The mobile app crashes when I open shared files.",
    "I want to downgrade my plan at the end of the cycle.",
    "Two-factor authentication isn't sending me a code.",
    "Our workspace admin left the company, need to transfer ownership.",
]

SUBJECT_TEMPLATES = [
    "Issue with my account",
    "Question about billing",
    "Problem accessing files",
    "Storage and plan question",
    "Need help with sharing",
    "Trouble with login",
    "Refund request",
    "Upload not working",
]


def sample_plan_tier(priority: str) -> str:
    # Business-tier customers skew toward higher priority, Free skews lower - but
    # it's a skew, not a rule, so the model can't just memorize "Business = URGENT".
    weights = {
        "URGENT": {"FREE": 0.05, "PRO": 0.30, "BUSINESS": 0.65},
        "HIGH": {"FREE": 0.10, "PRO": 0.45, "BUSINESS": 0.45},
        "MEDIUM": {"FREE": 0.30, "PRO": 0.45, "BUSINESS": 0.25},
        "LOW": {"FREE": 0.45, "PRO": 0.40, "BUSINESS": 0.15},
    }[priority]
    tiers, probs = zip(*weights.items())
    return random.choices(tiers, weights=probs, k=1)[0]


def sample_prior_ticket_count(priority: str) -> int:
    # Repeat contacts in the last 30 days correlate with higher priority (a pattern
    # of unresolved issues), with a long-ish tail and plenty of overlap between classes.
    base = {"URGENT": 3.2, "HIGH": 1.8, "MEDIUM": 0.9, "LOW": 0.4}[priority]
    return max(0, int(random.gauss(base, 1.3)))


def sample_account_age_days(priority: str) -> int:
    # Deliberately weak/no signal - newer accounts skew VERY slightly higher (more
    # onboarding friction) but mostly this is noise, same as in real support data
    # where account age alone rarely predicts urgency.
    base = {"URGENT": 240, "HIGH": 300, "MEDIUM": 380, "LOW": 420}[priority]
    return max(1, int(random.gauss(base, 250)))


def make_ticket_text(true_priority: str) -> tuple[str, str]:
    subject = random.choice(SUBJECT_TEMPLATES)
    topic = random.choice(TOPIC_PHRASES)
    tone_count = random.choice([1, 1, 2])
    tones = random.sample(TONE_PHRASES[true_priority], k=min(tone_count, len(TONE_PHRASES[true_priority])))
    body = " ".join([topic] + tones)
    return subject, body


def generate_row() -> dict:
    true_priority = random.choice(PRIORITIES)
    subject, body = make_ticket_text(true_priority)
    plan_tier = sample_plan_tier(true_priority)
    prior_tickets = sample_prior_ticket_count(true_priority)
    account_age = sample_account_age_days(true_priority)

    # Label noise: ~12% of tickets get bumped one level up or down from their "true"
    # tone-driven priority. This models real-world reality - two people describing an
    # equally bad situation don't always phrase it with the same urgency, and human
    # labelers (or a past triage process) don't always agree either. A dataset with
    # zero label noise is a strong hint it's synthetic in a way that won't transfer.
    label = true_priority
    if random.random() < 0.12:
        idx = PRIORITIES.index(true_priority)
        shift = random.choice([-1, 1])
        idx = min(max(idx + shift, 0), len(PRIORITIES) - 1)
        label = PRIORITIES[idx]

    return {
        "subject": subject,
        "body": body,
        "plan_tier": plan_tier,
        "account_age_days": account_age,
        "prior_ticket_count_30d": prior_tickets,
        "priority": label,
    }


if __name__ == "__main__":
    n_rows = 2200
    rows = [generate_row() for _ in range(n_rows)]
    df = pd.DataFrame(rows)
    df.to_csv("tickets_training_data.csv", index=False)

    print(f"Generated {len(df)} rows -> tickets_training_data.csv")
    print("\nClass balance:")
    print(df["priority"].value_counts())
    print("\nSample rows:")
    print(df.sample(5, random_state=1).to_string())
