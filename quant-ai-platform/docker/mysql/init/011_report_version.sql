USE quant_ai;

ALTER TABLE research_report
    ADD COLUMN version_no INT NOT NULL DEFAULT 1;

ALTER TABLE research_report_review_log
    ADD COLUMN version_no INT NOT NULL DEFAULT 1;

ALTER TABLE research_report_section
    ADD COLUMN version_no INT NOT NULL DEFAULT 1;
