/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intraloadbalancingft;

/**
 * @author JGUTIERRGARC
 */
public class WorkloadGeneratorGUI extends javax.swing.JFrame {


    /**
     * Creates new form WorkloadGeneratorGUI
     */
    private jade.wrapper.AgentContainer workloadGeneratorContainer;
    private Object[] workloadGeneratorContainerParams;


    public WorkloadGeneratorGUI(jade.wrapper.AgentContainer workloadGeneratorContainer, Object[] workloadGeneratorContainerParams) {
        this.workloadGeneratorContainer = workloadGeneratorContainer;
        this.workloadGeneratorContainerParams = workloadGeneratorContainerParams;
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonStart = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Workload Generator");
        setName("workloadGeneratorFrame"); // NOI18N

        buttonStart.setText("Start workload generation");
        buttonStart.setName("startButton"); // NOI18N
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addComponent(buttonStart)
                                .addContainerGap(30, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(buttonStart)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStartActionPerformed

        startSimulationRun();
        // TODO add your handling code here:
    }//GEN-LAST:event_buttonStartActionPerformed

    private void startSimulationRun() {
        try {
            workloadGeneratorContainer.createNewAgent("WorkloadGeneratorAgent", "intraloadbalancingft.WorkloadGeneratorAgent", workloadGeneratorContainerParams);
            workloadGeneratorContainer.getAgent("WorkloadGeneratorAgent").start();
        } catch (Exception e) {
            if (Consts.EXCEPTIONS)
                e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonStart;
    // End of variables declaration//GEN-END:variables
}
