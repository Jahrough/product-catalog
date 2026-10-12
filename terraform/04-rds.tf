resource "aws_db_subnet_group" "rds_subnet_group" {
  name       = "product-catalog-db-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Name = "product-db-subnet-group"
  }
}

resource "aws_security_group" "rds_sg" {
  name   = "product-rds-security-group"
  vpc_id = module.vpc.vpc_id

  ingress {
    description     = "Allow PostgreSQL access from VPC"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    cidr_blocks     = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "product-db"
  engine                 = "postgres"
  engine_version         = "15.4"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  max_allocated_storage  = 100
  db_name                = "productdb"
  username               = "postgres"
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.rds_subnet_group.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  skip_final_snapshot    = true
  publicly_accessible    = false

  tags = {
    Environment = "production"
  }
}