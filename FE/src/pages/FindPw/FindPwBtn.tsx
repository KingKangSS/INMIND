import styled from 'styled-components';
import { colors } from '../../theme/colors';
import Btn from '../../components/Btn';

const FindPwBtn = styled(Btn)`
    height: 50px;
    width: 400px;
    background-color: ${colors.okGreen};
    color: ${colors.white};
    font-size: 20px;
    font-weight: 700;
`;

export default FindPwBtn;
