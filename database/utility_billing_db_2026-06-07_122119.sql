--
-- PostgreSQL database dump
--

\restrict xYJwr4tCh68dghT7RTgC6gkUogTjhssazFUdWzRGI3pJYMgtX2aouORC4Kk41Lr

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fn_notify_bill_generated(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_notify_bill_generated() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_customer_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.bill_id = NEW.id AND n.notification_type = 'BILL_GENERATED'
    ) THEN
        SELECT c.full_name INTO v_customer_name
        FROM customers c WHERE c.id = NEW.customer_id;

        INSERT INTO notifications (
            customer_id, bill_id, message, notification_type, status, created_at, updated_at
        ) VALUES (
            NEW.customer_id,
            NEW.id,
            'Dear ' || v_customer_name || ',' || E'\n' ||
            'Your ' || NEW.billing_month || '/' || NEW.billing_year ||
            ' utility bill of ' || NEW.total_amount ||
            ' FRW has been successfully processed.',
            'BILL_GENERATED',
            'UNREAD',
            NOW(),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_notify_bill_generated() OWNER TO postgres;

--
-- Name: fn_notify_full_payment(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_notify_full_payment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_customer_name TEXT;
BEGIN
    IF NEW.status = 'PAID' AND OLD.status IS DISTINCT FROM 'PAID' THEN
        IF NOT EXISTS (
            SELECT 1 FROM notifications n
            WHERE n.bill_id = NEW.id AND n.notification_type = 'BILL_PAID'
        ) THEN
            SELECT c.full_name INTO v_customer_name
            FROM customers c WHERE c.id = NEW.customer_id;

            INSERT INTO notifications (
                customer_id, bill_id, message, notification_type, status, created_at, updated_at
            ) VALUES (
                NEW.customer_id,
                NEW.id,
                'Dear ' || v_customer_name || ',' || E'\n' ||
                'Your payment for ' || NEW.billing_month || '/' || NEW.billing_year ||
                ' utility bill has been received. Your bill is now fully paid.',
                'BILL_PAID',
                'UNREAD',
                NOW(),
                NOW()
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_notify_full_payment() OWNER TO postgres;

--
-- Name: sp_process_overdue_bills(); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.sp_process_overdue_bills()
    LANGUAGE plpgsql
    AS $$
DECLARE
    -- Active penalty configuration (most recent effective row).
    v_penalty_type   TEXT;
    v_penalty_value  NUMERIC(19,4);
    v_grace_days     INT;
    v_cutoff         DATE;
    v_penalty        NUMERIC(19,4);
    v_current_penalty NUMERIC(19,4);
    v_current_outstanding NUMERIC(19,4);
    v_customer_name  TEXT;

    -- CURSOR over all bills that are past due and not yet penalised.
    bill_cursor CURSOR FOR
        SELECT * FROM bills b
        WHERE b.status IN ('APPROVED', 'PARTIALLY_PAID')
          AND b.due_date < v_cutoff
        FOR UPDATE;
    bill_row bills%ROWTYPE;
BEGIN
    SELECT pc.penalty_type, pc.amount_or_percentage, pc.grace_period_days
      INTO v_penalty_type, v_penalty_value, v_grace_days
    FROM penalty_configs pc
    WHERE pc.active = TRUE
      AND pc.effective_from <= CURRENT_DATE
      AND (pc.effective_to IS NULL OR pc.effective_to >= CURRENT_DATE)
    ORDER BY pc.id DESC
    LIMIT 1;

    v_grace_days := COALESCE(v_grace_days, 0);
    v_cutoff := CURRENT_DATE - v_grace_days;

    OPEN bill_cursor;
    LOOP
        FETCH bill_cursor INTO bill_row;
        EXIT WHEN NOT FOUND;

        -- Apply the penalty at most once.
        IF bill_row.penalty_amount = 0 AND v_penalty_type IS NOT NULL THEN
            IF v_penalty_type = 'FIXED' THEN
                v_penalty := ROUND(v_penalty_value, 2);
            ELSE
                v_penalty := ROUND(bill_row.outstanding_balance * v_penalty_value / 100, 2);
            END IF;

            UPDATE bills
               SET penalty_amount      = v_penalty,
                   total_amount        = total_amount + v_penalty,
                   outstanding_balance = outstanding_balance + v_penalty,
                   status              = 'OVERDUE',
                   updated_at          = NOW()
             WHERE id = bill_row.id
             RETURNING penalty_amount, outstanding_balance
                  INTO v_current_penalty, v_current_outstanding;
        ELSE
            UPDATE bills
               SET status = 'OVERDUE', updated_at = NOW()
             WHERE id = bill_row.id
             RETURNING penalty_amount, outstanding_balance
                  INTO v_current_penalty, v_current_outstanding;
        END IF;

        SELECT c.full_name INTO v_customer_name FROM customers c WHERE c.id = bill_row.customer_id;

        INSERT INTO notifications (
            customer_id, bill_id, message, notification_type, status, created_at, updated_at
        ) VALUES (
            bill_row.customer_id,
            bill_row.id,
            'Dear ' || v_customer_name || ',' || E'\n' ||
            'Your ' || bill_row.billing_month || '/' || bill_row.billing_year ||
            ' utility bill is overdue. Penalty applied: ' || COALESCE(v_current_penalty, 0) ||
            ' FRW. Remaining amount to be paid is ' || COALESCE(v_current_outstanding, bill_row.outstanding_balance) || ' FRW.',
            'BILL_OVERDUE',
            'UNREAD',
            NOW(),
            NOW()
        );
    END LOOP;
    CLOSE bill_cursor;
END;
$$;


ALTER PROCEDURE public.sp_process_overdue_bills() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bills; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bills (
    amount_paid numeric(19,4) NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    consumption numeric(19,4) NOT NULL,
    due_date date NOT NULL,
    fixed_charge numeric(19,4) NOT NULL,
    outstanding_balance numeric(19,4) NOT NULL,
    penalty_amount numeric(19,4) NOT NULL,
    tariff_amount numeric(19,4) NOT NULL,
    tax_amount numeric(19,4) NOT NULL,
    total_amount numeric(19,4) NOT NULL,
    approved_at timestamp(6) without time zone,
    approved_by bigint,
    created_at timestamp(6) without time zone,
    customer_id bigint NOT NULL,
    id bigint NOT NULL,
    meter_id bigint NOT NULL,
    meter_reading_id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    bill_reference character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT bills_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.bills OWNER TO postgres;

--
-- Name: bills_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.bills ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customers (
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint,
    address character varying(255) NOT NULL,
    email character varying(255),
    full_name character varying(255) NOT NULL,
    national_id character varying(255) NOT NULL,
    phone_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.customers OWNER TO postgres;

--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.customers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_verification_otps; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.email_verification_otps (
    used boolean NOT NULL,
    code character varying(6) NOT NULL,
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    used_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);


ALTER TABLE public.email_verification_otps OWNER TO postgres;

--
-- Name: email_verification_otps_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.email_verification_otps ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.email_verification_otps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: fixed_charges; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fixed_charges (
    active boolean NOT NULL,
    amount numeric(19,4) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    version integer NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    meter_type character varying(255) NOT NULL,
    CONSTRAINT fixed_charges_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[])))
);


ALTER TABLE public.fixed_charges OWNER TO postgres;

--
-- Name: fixed_charges_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.fixed_charges ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.fixed_charges_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meter_readings (
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    consumption numeric(19,4) NOT NULL,
    current_reading numeric(19,4) NOT NULL,
    previous_reading numeric(19,4) NOT NULL,
    reading_date date NOT NULL,
    captured_by bigint NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    meter_id bigint NOT NULL,
    updated_at timestamp(6) without time zone
);


ALTER TABLE public.meter_readings OWNER TO postgres;

--
-- Name: meter_readings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.meter_readings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meter_readings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meters (
    installation_date date NOT NULL,
    created_at timestamp(6) without time zone,
    customer_id bigint NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    meter_number character varying(255) NOT NULL,
    meter_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT meters_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT meters_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.meters OWNER TO postgres;

--
-- Name: meters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.meters ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    bill_id bigint,
    created_at timestamp(6) without time zone,
    customer_id bigint NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    message text NOT NULL,
    notification_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT notifications_notification_type_check CHECK (((notification_type)::text = ANY ((ARRAY['BILL_GENERATED'::character varying, 'PAYMENT_CONFIRMED'::character varying, 'BILL_PAID'::character varying, 'BILL_OVERDUE'::character varying])::text[]))),
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['UNREAD'::character varying, 'READ'::character varying])::text[])))
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.notifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    amount_paid numeric(19,4) NOT NULL,
    payment_date date NOT NULL,
    bill_id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    customer_id bigint NOT NULL,
    id bigint NOT NULL,
    recorded_by bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    payment_method character varying(255) NOT NULL,
    payment_reference character varying(255) NOT NULL,
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying, 'CARD'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.payments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: penalty_configs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.penalty_configs (
    active boolean NOT NULL,
    amount_or_percentage numeric(19,4) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    grace_period_days integer NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    name character varying(255) NOT NULL,
    penalty_type character varying(255) NOT NULL,
    CONSTRAINT penalty_configs_penalty_type_check CHECK (((penalty_type)::text = ANY ((ARRAY['FIXED'::character varying, 'PERCENTAGE'::character varying])::text[])))
);


ALTER TABLE public.penalty_configs OWNER TO postgres;

--
-- Name: penalty_configs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.penalty_configs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.penalty_configs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: revoked_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.revoked_tokens (
    created_at timestamp(6) without time zone,
    expires_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    token text NOT NULL
);


ALTER TABLE public.revoked_tokens OWNER TO postgres;

--
-- Name: revoked_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.revoked_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.revoked_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariff_tiers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariff_tiers (
    max_units numeric(19,4),
    min_units numeric(19,4) NOT NULL,
    rate_per_unit numeric(19,4) NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    tariff_id bigint NOT NULL,
    updated_at timestamp(6) without time zone
);


ALTER TABLE public.tariff_tiers OWNER TO postgres;

--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tariff_tiers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariff_tiers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariffs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariffs (
    active boolean NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    rate_per_unit numeric(19,4),
    version integer NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    meter_type character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    tariff_type character varying(255) NOT NULL,
    CONSTRAINT tariffs_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT tariffs_tariff_type_check CHECK (((tariff_type)::text = ANY ((ARRAY['FLAT'::character varying, 'TIERED'::character varying])::text[])))
);


ALTER TABLE public.tariffs OWNER TO postgres;

--
-- Name: tariffs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tariffs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariffs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tax_configs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tax_configs (
    active boolean NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    percentage numeric(10,4) NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    name character varying(255) NOT NULL
);


ALTER TABLE public.tax_configs OWNER TO postgres;

--
-- Name: tax_configs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tax_configs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tax_configs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    email_verified boolean DEFAULT false NOT NULL,
    created_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    phone_number character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_OPERATOR'::character varying, 'ROLE_FINANCE'::character varying, 'ROLE_CUSTOMER'::character varying])::text[]))),
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bills (amount_paid, billing_month, billing_year, consumption, due_date, fixed_charge, outstanding_balance, penalty_amount, tariff_amount, tax_amount, total_amount, approved_at, approved_by, created_at, customer_id, id, meter_id, meter_reading_id, updated_at, bill_reference, status) FROM stdin;
0.0000	3	2026	250.0000	2026-06-22	1000.0000	89680.0000	0.0000	75000.0000	13680.0000	89680.0000	\N	\N	2026-06-07 12:04:50.17164	2	1	3	1	2026-06-07 12:04:50.17164	BILL-2026-03-000001	PENDING_APPROVAL
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customers (created_at, id, updated_at, user_id, address, email, full_name, national_id, phone_number, status) FROM stdin;
2026-06-07 11:42:51.206581	1	2026-06-07 11:42:51.206581	4	Kigali, Rwanda	customer@utility.rw	Default Customer	1234567890123456	+250780000004	ACTIVE
2026-06-07 11:55:24.328204	2	2026-06-07 11:59:05.343923	7	Kigali, Kicukiro, Niboye	ingabo1234@gmail.com	Ngabo customer	1199080076543210	+250781234567	ACTIVE
\.


--
-- Data for Name: email_verification_otps; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.email_verification_otps (used, code, created_at, expires_at, id, updated_at, used_at, user_id) FROM stdin;
t	533465	2026-06-07 11:52:21.064515	2026-06-07 12:02:21.062378	1	2026-06-07 11:53:39.408643	2026-06-07 11:53:39.396431	7
\.


--
-- Data for Name: fixed_charges; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.fixed_charges (active, amount, effective_from, effective_to, version, created_at, id, updated_at, meter_type) FROM stdin;
t	1000.0000	2025-06-07	\N	1	2026-06-07 11:42:51.235361	1	2026-06-07 11:42:51.235361	WATER
t	1500.0000	2025-06-07	\N	1	2026-06-07 11:42:51.240606	2	2026-06-07 11:42:51.240606	ELECTRICITY
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meter_readings (billing_month, billing_year, consumption, current_reading, previous_reading, reading_date, captured_by, created_at, id, meter_id, updated_at) FROM stdin;
3	2026	250.0000	250.0000	0.0000	2026-03-05	5	2026-06-07 12:04:50.123112	1	3	2026-06-07 12:04:50.123112
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meters (installation_date, created_at, customer_id, id, updated_at, meter_number, meter_type, status) FROM stdin;
2025-12-07	2026-06-07 11:42:51.219402	1	1	2026-06-07 11:42:51.219402	WATER-0001	WATER	ACTIVE
2025-12-07	2026-06-07 11:42:51.22515	1	2	2026-06-07 11:42:51.22515	ELEC-0001	ELECTRICITY	ACTIVE
2026-01-15	2026-06-07 12:00:35.9479	2	3	2026-06-07 12:00:35.9479	WTR-2026-0001	WATER	ACTIVE
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (bill_id, created_at, customer_id, id, updated_at, message, notification_type, status) FROM stdin;
1	2026-06-07 12:04:50.10161	2	1	2026-06-07 12:04:50.10161	Dear Ngabo customer,\nYour 3/2026 utility bill of 89680.0000 FRW has been successfully processed.	BILL_GENERATED	UNREAD
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (amount_paid, payment_date, bill_id, created_at, customer_id, id, recorded_by, updated_at, payment_method, payment_reference) FROM stdin;
\.


--
-- Data for Name: penalty_configs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.penalty_configs (active, amount_or_percentage, effective_from, effective_to, grace_period_days, created_at, id, updated_at, name, penalty_type) FROM stdin;
t	5.0000	2025-06-07	\N	5	2026-06-07 11:42:51.2466	1	2026-06-07 11:42:51.2466	Late Payment Penalty	PERCENTAGE
\.


--
-- Data for Name: revoked_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.revoked_tokens (created_at, expires_at, id, updated_at, token) FROM stdin;
\.


--
-- Data for Name: tariff_tiers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariff_tiers (max_units, min_units, rate_per_unit, created_at, id, tariff_id, updated_at) FROM stdin;
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariffs (active, effective_from, effective_to, rate_per_unit, version, created_at, id, updated_at, meter_type, name, tariff_type) FROM stdin;
t	2025-06-07	\N	300.0000	1	2026-06-07 11:42:51.228149	1	2026-06-07 11:42:51.228149	WATER	Water Flat Tariff	FLAT
t	2025-06-07	\N	500.0000	1	2026-06-07 11:42:51.23236	2	2026-06-07 11:42:51.23236	ELECTRICITY	Electricity Flat Tariff	FLAT
\.


--
-- Data for Name: tax_configs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tax_configs (active, effective_from, effective_to, percentage, created_at, id, updated_at, name) FROM stdin;
t	2025-06-07	\N	18.0000	2026-06-07 11:42:51.243641	1	2026-06-07 11:42:51.243641	VAT
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (email_verified, created_at, id, updated_at, email, full_name, password, phone_number, role, status) FROM stdin;
t	2026-06-07 11:42:50.967631	1	2026-06-07 11:42:50.967631	admin@utility.rw	System Admin	$2a$10$tiMgJyHDzJpJdyUCC4SdFu5GKHMW3b7kU9.8mIlju2taKhYeaOrA.	+250780000001	ROLE_ADMIN	ACTIVE
t	2026-06-07 11:42:51.062948	2	2026-06-07 11:42:51.062948	operator@utility.rw	Meter Operator	$2a$10$HArJ2hhyne1Mj4I9cL7UeOG6THZSbMIndqWgWZeWzw3Qr6Wpx7n1y	+250780000002	ROLE_OPERATOR	ACTIVE
t	2026-06-07 11:42:51.131578	3	2026-06-07 11:42:51.132502	finance@utility.rw	Finance Officer	$2a$10$S8UPLQ0zX/nijsrEASQWcOhFfy9/1SAJGnvBiZeppz6Jg37V8nIDe	+250780000003	ROLE_FINANCE	ACTIVE
t	2026-06-07 11:42:51.198599	4	2026-06-07 11:42:51.198599	customer@utility.rw	Default Customer	$2a$10$xD7kvzDe/qmKx2ZdmIVqN.RckmcXxGzegpAAVxyiK/hlyMF9DIr.i	+250780000004	ROLE_CUSTOMER	ACTIVE
t	2026-06-07 11:48:46.522443	5	2026-06-07 11:48:46.522443	raphael@gmail.com	Raphael Nibishaka	$2a$10$kuwQx.d.pqvT4z0ycmdZXeW.rJGIsgxdTbJgAlQyP1TNj4/hld8Ya	+250782345678	ROLE_OPERATOR	ACTIVE
t	2026-06-07 11:49:36.002017	6	2026-06-07 11:49:36.002017	andrew@gmail.com	andrew manzi	$2a$10$xK7CFsfHKohaJf7XomFjl.kWW0Rav/4.kFk30CcPyGiWjhHDGlwCW	+250782345678	ROLE_FINANCE	ACTIVE
t	2026-06-07 11:52:21.049505	7	2026-06-07 11:53:39.405564	ingabo1234@gmail.com	Ngabo customer	$2a$10$T7YwI9/ELA/Dol1uOfz8XezSb/68cLzhA5qEkI6rcL/xpEHieXhi.	+250781234567	ROLE_CUSTOMER	ACTIVE
\.


--
-- Name: bills_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bills_id_seq', 1, true);


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.customers_id_seq', 2, true);


--
-- Name: email_verification_otps_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.email_verification_otps_id_seq', 1, true);


--
-- Name: fixed_charges_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.fixed_charges_id_seq', 2, true);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.meter_readings_id_seq', 1, true);


--
-- Name: meters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.meters_id_seq', 3, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notifications_id_seq', 1, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.payments_id_seq', 1, false);


--
-- Name: penalty_configs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.penalty_configs_id_seq', 1, true);


--
-- Name: revoked_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.revoked_tokens_id_seq', 1, false);


--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tariff_tiers_id_seq', 1, false);


--
-- Name: tariffs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tariffs_id_seq', 2, true);


--
-- Name: tax_configs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tax_configs_id_seq', 1, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 7, true);


--
-- Name: bills bills_bill_reference_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_bill_reference_key UNIQUE (bill_reference);


--
-- Name: bills bills_meter_id_billing_month_billing_year_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_meter_id_billing_month_billing_year_key UNIQUE (meter_id, billing_month, billing_year);


--
-- Name: bills bills_meter_reading_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_meter_reading_id_key UNIQUE (meter_reading_id);


--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- Name: customers customers_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_email_key UNIQUE (email);


--
-- Name: customers customers_national_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_national_id_key UNIQUE (national_id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: customers customers_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_user_id_key UNIQUE (user_id);


--
-- Name: email_verification_otps email_verification_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT email_verification_otps_pkey PRIMARY KEY (id);


--
-- Name: fixed_charges fixed_charges_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fixed_charges
    ADD CONSTRAINT fixed_charges_pkey PRIMARY KEY (id);


--
-- Name: meter_readings meter_readings_meter_id_billing_month_billing_year_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_meter_id_billing_month_billing_year_key UNIQUE (meter_id, billing_month, billing_year);


--
-- Name: meter_readings meter_readings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_pkey PRIMARY KEY (id);


--
-- Name: meters meters_meter_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_meter_number_key UNIQUE (meter_number);


--
-- Name: meters meters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: payments payments_payment_reference_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_payment_reference_key UNIQUE (payment_reference);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: penalty_configs penalty_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.penalty_configs
    ADD CONSTRAINT penalty_configs_pkey PRIMARY KEY (id);


--
-- Name: revoked_tokens revoked_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_pkey PRIMARY KEY (id);


--
-- Name: revoked_tokens revoked_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.revoked_tokens
    ADD CONSTRAINT revoked_tokens_token_key UNIQUE (token);


--
-- Name: tariff_tiers tariff_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT tariff_tiers_pkey PRIMARY KEY (id);


--
-- Name: tariffs tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_pkey PRIMARY KEY (id);


--
-- Name: tax_configs tax_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tax_configs
    ADD CONSTRAINT tax_configs_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: bills trg_notify_bill_generated; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_notify_bill_generated AFTER INSERT ON public.bills FOR EACH ROW EXECUTE FUNCTION public.fn_notify_bill_generated();


--
-- Name: bills trg_notify_full_payment; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_notify_full_payment AFTER UPDATE OF status ON public.bills FOR EACH ROW EXECUTE FUNCTION public.fn_notify_full_payment();


--
-- Name: notifications fk28wkse7ifkf33ani76n85rglf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk28wkse7ifkf33ani76n85rglf FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: notifications fk30dp6ycner3dgso3scgc9vghy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk30dp6ycner3dgso3scgc9vghy FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: payments fk45dp0030s8e3myd8n6ky4e79g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk45dp0030s8e3myd8n6ky4e79g FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fk6trf19k7f06hj0o5y4myngod0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk6trf19k7f06hj0o5y4myngod0 FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: tariff_tiers fk90xk77nst2f05cmrod4d5q42h; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT fk90xk77nst2f05cmrod4d5q42h FOREIGN KEY (tariff_id) REFERENCES public.tariffs(id);


--
-- Name: payments fk9565r6579khpdjxnyla0l2ycd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk9565r6579khpdjxnyla0l2ycd FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: payments fka6nye1mj985yt9mg3umq45fnq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fka6nye1mj985yt9mg3umq45fnq FOREIGN KEY (recorded_by) REFERENCES public.users(id);


--
-- Name: meter_readings fkbvqyhi6ytu4npiu57a4k4ammf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fkbvqyhi6ytu4npiu57a4k4ammf FOREIGN KEY (captured_by) REFERENCES public.users(id);


--
-- Name: meters fkdgg79dhtsr0eumbce7ipw58lj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fkdgg79dhtsr0eumbce7ipw58lj FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: email_verification_otps fkesbi756wm002lbh9cptn5id02; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT fkesbi756wm002lbh9cptn5id02 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: bills fkfes5685l6y4urtsc0cq3cobo1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkfes5685l6y4urtsc0cq3cobo1 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fkjktiv3utpgao93xx3homxrhuf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkjktiv3utpgao93xx3homxrhuf FOREIGN KEY (meter_reading_id) REFERENCES public.meter_readings(id);


--
-- Name: meter_readings fknalaulqjlf29g1dlukdeyg0g4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fknalaulqjlf29g1dlukdeyg0g4 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fkoy9sc2dmxj2qwjeiiilf3yuxp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkoy9sc2dmxj2qwjeiiilf3yuxp FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customers fkrh1g1a20omjmn6kurd35o3eit; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fkrh1g1a20omjmn6kurd35o3eit FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict xYJwr4tCh68dghT7RTgC6gkUogTjhssazFUdWzRGI3pJYMgtX2aouORC4Kk41Lr

