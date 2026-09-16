CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price > 0),
    total_amount NUMERIC(19, 2) NOT NULL CHECK (total_amount > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
