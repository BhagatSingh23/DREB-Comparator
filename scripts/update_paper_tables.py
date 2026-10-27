import json
import os
import re

def update_tables():
    # 1. Read E11 README
    with open("results/e11/README.md") as f:
        e11_text = f.read()

    # 2. Read E12 README
    with open("results/e12/README.md") as f:
        e12_text = f.read()

    # 3. Read PAPER_TABLES.md
    with open("results/PAPER_TABLES.md") as f:
        paper_text = f.read()

    # Check if section 11 already exists in paper_text
    if "## 11. Experiment E11:" in paper_text:
        paper_text = paper_text[:paper_text.index("## 11. Experiment E11:")]

    # Format Section 11 from e11_text
    s11 = "## 11. Experiment E11: Adaptive Credit-Farming Adversary Evaluation\n\n"
    # Extract Table 11.1, Table 11.2, Table 11.3
    t11_1_idx = e11_text.index("## 2. Table 11.1:")
    s11 += e11_text[t11_1_idx:] + "\n"

    # Format Section 12 from e12_text
    s12 = "## 12. Experiment E12: DREB v2 Gain Sweep & Ablation Completion\n\n"
    t12_1_idx = e12_text.index("## 2. Table 12.1:")
    s12 += e12_text[t12_1_idx:] + "\n"

    new_paper_text = paper_text.rstrip() + "\n\n---\n\n" + s11.strip() + "\n\n---\n\n" + s12.strip() + "\n"

    with open("results/PAPER_TABLES.md", "w") as f:
        f.write(new_paper_text)
    print("Appended Sections 11 and 12 to results/PAPER_TABLES.md successfully.")

if __name__ == "__main__":
    update_tables()
