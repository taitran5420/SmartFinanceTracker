resource "aws_security_group" "smartfinancetracker" {
  name        = "smartfinancetracker-sg"
  description = "Security group for SmartFinanceTracker EC2 instance"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0", var.my_ip_cidr]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "App"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.my_ip_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_key_pair" "smartfinancetracker" {
  key_name = "smartfinancetracker-poc"

  public_key = var.smart_finance_public_key
}

resource "aws_instance" "smartfinancetracker" {
  ami           = "ami-0532913178263be11"
  instance_type = "c7i-flex.large"
  key_name      = aws_key_pair.smartfinancetracker.key_name

  vpc_security_group_ids = [aws_security_group.smartfinancetracker.id]

  iam_instance_profile = aws_iam_instance_profile.ec2-ecr-pull-role.name

  user_data = filebase64("${path.module}/../deploy/ec2-user-data.sh")

  tags = {
    Name = aws_key_pair.smartfinancetracker.key_name
  }
}

resource "aws_eip" "smartfinancetracker" {
  domain = "vpc"
}

resource "aws_eip_association" "smartfinancetracker" {
  instance_id   = aws_instance.smartfinancetracker.id
  allocation_id = aws_eip.smartfinancetracker.id
}
