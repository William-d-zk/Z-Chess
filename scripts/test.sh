#!/bin/bash
#
# Z-Chess 统一测试入口脚本
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_help() {
    echo "Z-Chess 测试脚本入口"
    echo ""
    echo "用法: $0 <测试类型> [参数]"
    echo ""
    echo "测试类型:"
    echo "  docker      Docker 测试集群管理"
    echo "  e2e         端到端测试"
    echo "  tls         TLS 测试和证书管理"
    echo ""
    echo "示例:"
    echo "  $0 docker start          # 启动 Docker 测试集群"
    echo "  $0 docker status         # 查看集群状态"
    echo "  $0 e2e                   # 运行端到端测试"
    echo "  $0 tls                   # 生成 TLS 证书"
    echo ""
    echo "详细帮助:"
    echo "  $0 docker --help         # Docker 测试帮助"
    echo "  $0 e2e --help            # E2E 测试帮助"
}

main() {
    local test_type="$1"
    shift

    case "$test_type" in
        docker)
            exec "${SCRIPT_DIR}/test/docker/test-cluster.sh" "$@"
            ;;
        e2e)
            exec "${SCRIPT_DIR}/test/e2e/e2e-test.sh" "$@"
            ;;
        tls)
            if [ $# -eq 0 ]; then
                echo "生成 TLS 证书..."
                exec "${SCRIPT_DIR}/test/tls/generate-ssl-certs.sh"
            else
                exec "${SCRIPT_DIR}/test/tls/generate-ssl-certs.sh" "$@"
            fi
            ;;
        help|--help|-h)
            print_help
            ;;
        *)
            echo "错误: 未知的测试类型 '$test_type'"
            echo ""
            print_help
            exit 1
            ;;
    esac
}

main "$@"
